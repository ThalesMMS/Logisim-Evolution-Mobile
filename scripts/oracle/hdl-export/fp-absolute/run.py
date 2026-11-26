#!/usr/bin/env python3
"""Simulate Evolution's FP Absolute HDL (PR 3174) on the vectors of Mobile's fp_absolute fixtures.

The script runs FpAbsoluteEvolutionOracle against an Evolution JAR, wraps each generated module body
in the entity or module that Evolution's generator declares (dataIn, result, fpError), and checks it
with Icarus Verilog and a VHDL-2008 simulator. The expected values are computed here from the IEEE
field layout, not from either generator.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

HERE = Path(__file__).resolve().parent
FRACTION_BITS = {8: 3, 16: 10, 32: 23, 64: 52}


def run(command, directory, timeout=120):
    result = subprocess.run(command, cwd=directory, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=timeout)
    if result.returncode:
        raise RuntimeError(f"Command failed ({result.returncode}): {command}\n{result.stdout}")
    return result.stdout


def vectors(width):
    fraction = FRACTION_BITS[width]
    exponent = width - 1 - fraction
    bias = (1 << (exponent - 1)) - 1
    sign = 1 << (width - 1)
    infinity = ((1 << exponent) - 1) << fraction
    quiet_nan = infinity | (1 << (fraction - 1)) | 1
    signaling_nan = infinity | 1
    magnitudes = [0, 1, (bias << fraction) | (1 << (fraction - 1)), (bias + 1) << fraction,
                  infinity - 1, infinity, quiet_nan, signaling_nan]
    rows = []
    for magnitude in magnitudes:
        error = 1 if magnitude in (quiet_nan, signaling_nan) else 0
        rows += [(magnitude, magnitude, error), (magnitude | sign, magnitude, error)]
    return rows


def binary(value, width):
    return format(value, f"0{width}b")


def verilog_design(name, body, width):
    return (f"module {name}(dataIn, result, fpError);\n  input [{width - 1}:0] dataIn;\n"
            f"  output [{width - 1}:0] result;\n  output fpError;\n{body}\nendmodule\n")


def verilog_bench(name, width):
    lines = ["`timescale 1ns/1ps", "module tb;", f"  reg [{width - 1}:0] dataIn;",
             f"  wire [{width - 1}:0] result;", "  wire fpError;",
             f"  {name} dut(.dataIn(dataIn), .result(result), .fpError(fpError));", "  initial begin"]
    for index, (value, expected, error) in enumerate(vectors(width)):
        lines += [f"    dataIn = {width}'b{binary(value, width)}; #1;",
                  f"    if (result !== {width}'b{binary(expected, width)} || fpError !== 1'b{error}) begin",
                  f"      $display(\"FAIL vector {index} %b %b\", result, fpError); $finish; end"]
    lines += [f"    $display(\"FP_ABSOLUTE_PASS {name}\");", "    $finish;", "  end", "endmodule"]
    return "\n".join(lines) + "\n"


def vhdl_design(name, body, width):
    return ("library ieee;\nuse ieee.std_logic_1164.all;\nuse ieee.numeric_std.all;\n\n"
            f"entity {name} is\n  port (dataIn : in std_logic_vector({width - 1} downto 0);\n"
            f"        result : out std_logic_vector({width - 1} downto 0);\n        fpError : out std_logic);\n"
            f"end entity;\n\narchitecture platformIndependent of {name} is\nbegin\n{body}\nend architecture;\n")


def vhdl_bench(name, width):
    lines = ["library ieee;", "use ieee.std_logic_1164.all;", "entity tb is end entity;",
             "architecture sim of tb is",
             f"  signal dataIn, result : std_logic_vector({width - 1} downto 0);", "  signal fpError : std_logic;",
             "begin", f"  dut : entity work.{name} port map (dataIn => dataIn, result => result, fpError => fpError);",
             "  process begin"]
    for index, (value, expected, error) in enumerate(vectors(width)):
        lines += [f"    dataIn <= \"{binary(value, width)}\"; wait for 1 ns;",
                  f"    assert result = \"{binary(expected, width)}\" and fpError = '{error}' "
                  f"report \"FAIL vector {index}\" severity failure;"]
    lines += [f"    report \"FP_ABSOLUTE_PASS {name}\";", "    wait;", "  end process;", "end architecture;"]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--evolution-jar", type=Path, required=True)
    parser.add_argument("--evolution-revision", required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--iverilog", default="iverilog")
    parser.add_argument("--vvp", default="vvp")
    parser.add_argument("--vhdl-simulator", choices=("nvc", "ghdl"), default="nvc")
    parser.add_argument("--vhdl-tool", help="Path of the VHDL simulator executable")
    args = parser.parse_args()
    out = args.out.resolve()
    if out.exists():
        shutil.rmtree(out)
    classes, bodies = out / "classes", out / "bodies"
    classes.mkdir(parents=True)
    jar = str(args.evolution_jar.resolve())
    run(["javac", "-d", str(classes), "-cp", jar, str(HERE / "FpAbsoluteEvolutionOracle.java")], out)
    run(["java", "-Djava.awt.headless=true",
         "-Djava.util.prefs.PreferencesFactory=FpAbsoluteEvolutionOracle$MemoryPreferencesFactory",
         "-cp", f"{classes}{os.pathsep}{jar}", "FpAbsoluteEvolutionOracle", str(bodies)], out)
    vhdl = args.vhdl_simulator
    tools = {"iverilog": shutil.which(args.iverilog), "vvp": shutil.which(args.vvp),
             vhdl: shutil.which(args.vhdl_tool or vhdl)}
    if not all(tools.values()):
        raise SystemExit(f"Install Icarus Verilog (iverilog/vvp) and {vhdl.upper()}, or pass their paths.")
    results = []
    for width in sorted(FRACTION_BITS):
        for language in ("verilog", "vhdl"):
            name, body = (bodies / f"{language}-{width}.body").read_text().split("\n", 1)
            directory = out / f"{language}-{width}"
            directory.mkdir()
            if language == "verilog":
                (directory / "design.v").write_text(verilog_design(name, body, width))
                (directory / "tb.v").write_text(verilog_bench(name, width))
                output = run([tools["iverilog"], "-g2012", "-s", "tb", "-o", "sim.vvp", "design.v", "tb.v"], directory)
                output += run([tools["vvp"], "sim.vvp"], directory)
            else:
                (directory / "design.vhd").write_text(vhdl_design(name, body, width))
                (directory / "tb.vhd").write_text(vhdl_bench(name, width))
                if vhdl == "ghdl":
                    output = run([tools[vhdl], "-a", "--std=08", "design.vhd", "tb.vhd"], directory)
                    output += run([tools[vhdl], "-e", "--std=08", "tb"], directory)
                    output += run([tools[vhdl], "-r", "--std=08", "tb"], directory)
                else:
                    output = run([tools[vhdl], "--std=2008", "-a", "design.vhd", "tb.vhd"], directory)
                    output += run([tools[vhdl], "--std=2008", "-e", "tb"], directory)
                    output += run([tools[vhdl], "--std=2008", "-r", "tb"], directory)
            (directory / "tool-output.log").write_text(output)
            if f"FP_ABSOLUTE_PASS {name}" not in output:
                raise RuntimeError(f"{language}/{width} did not pass:\n{output}")
            results.append({"language": language, "width": width, "module": name,
                            "vectors": len(vectors(width)), "passed": True})
    report = {"schema": 1, "evolution_revision": args.evolution_revision,
              "evolution_jar_sha256": hashlib.sha256(args.evolution_jar.read_bytes()).hexdigest(),
              "tools": {"iverilog": run([tools["iverilog"], "-V"], out).splitlines()[0],
                        vhdl: run([tools[vhdl], "--version"], out).splitlines()[0]},
              "designs": results, "comparisons": sum(2 * r["vectors"] for r in results)}
    (out / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"PASS: {len(results)} Evolution FP Absolute designs; {report['comparisons']} output comparisons")


if __name__ == "__main__":
    main()
