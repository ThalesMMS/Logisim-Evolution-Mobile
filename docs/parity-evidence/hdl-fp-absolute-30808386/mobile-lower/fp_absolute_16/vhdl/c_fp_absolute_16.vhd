-- Generated deterministically from circuit fp_absolute_16.
-- VHDL-2008. Defined binary inputs; simulator delays are not represented.
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity c_fp_absolute_16 is
  port (
    p_input_0 : in std_logic_vector(15 downto 0);
    p_output_0 : out std_logic_vector(15 downto 0);
    p_output_1 : out std_logic_vector(0 downto 0)
  );
end entity;

architecture rtl of c_fp_absolute_16 is
  function hdl_choose(condition : boolean; yes_value, no_value : std_logic_vector) return std_logic_vector is
  begin
    if condition then return yes_value; else return no_value; end if;
  end function;
  signal s_0000 : std_logic;
  signal s_0001 : std_logic;
  signal s_0002 : std_logic;
  signal s_0003 : std_logic;
  signal s_0004 : std_logic;
  signal s_0005 : std_logic;
  signal s_0006 : std_logic;
  signal s_0007 : std_logic;
  signal s_0008 : std_logic;
  signal s_0009 : std_logic;
  signal s_0010 : std_logic;
  signal s_0011 : std_logic;
  signal s_0012 : std_logic;
  signal s_0013 : std_logic;
  signal s_0014 : std_logic;
  signal s_0015 : std_logic;
  signal s_0016 : std_logic;
  signal s_0017 : std_logic;
  signal s_0018 : std_logic;
  signal s_0019 : std_logic;
  signal s_0020 : std_logic;
  signal s_0021 : std_logic;
  signal s_0022 : std_logic;
  signal s_0023 : std_logic;
  signal s_0024 : std_logic;
  signal s_0025 : std_logic;
  signal s_0026 : std_logic;
  signal s_0027 : std_logic;
  signal s_0028 : std_logic;
  signal s_0029 : std_logic;
  signal s_0030 : std_logic;
  signal s_0031 : std_logic;
  signal s_0032 : std_logic;
  signal v_abs_out0 : std_logic_vector(15 downto 0);
  signal v_abs_out1 : std_logic_vector(0 downto 0);
begin
  s_0000 <= p_input_0(0);
  s_0001 <= p_input_0(1);
  s_0002 <= p_input_0(2);
  s_0003 <= p_input_0(3);
  s_0004 <= p_input_0(4);
  s_0005 <= p_input_0(5);
  s_0006 <= p_input_0(6);
  s_0007 <= p_input_0(7);
  s_0008 <= p_input_0(8);
  s_0009 <= p_input_0(9);
  s_0010 <= p_input_0(10);
  s_0011 <= p_input_0(11);
  s_0012 <= p_input_0(12);
  s_0013 <= p_input_0(13);
  s_0014 <= p_input_0(14);
  s_0015 <= p_input_0(15);
  p_output_0(0) <= s_0017;
  p_output_0(1) <= s_0018;
  p_output_0(2) <= s_0019;
  p_output_0(3) <= s_0020;
  p_output_0(4) <= s_0021;
  p_output_0(5) <= s_0022;
  p_output_0(6) <= s_0023;
  p_output_0(7) <= s_0024;
  p_output_0(8) <= s_0025;
  p_output_0(9) <= s_0026;
  p_output_0(10) <= s_0027;
  p_output_0(11) <= s_0028;
  p_output_0(12) <= s_0029;
  p_output_0(13) <= s_0030;
  p_output_0(14) <= s_0031;
  p_output_0(15) <= s_0032;
  p_output_1(0) <= s_0016;
  s_0017 <= v_abs_out0(0);
  s_0018 <= v_abs_out0(1);
  s_0019 <= v_abs_out0(2);
  s_0020 <= v_abs_out0(3);
  s_0021 <= v_abs_out0(4);
  s_0022 <= v_abs_out0(5);
  s_0023 <= v_abs_out0(6);
  s_0024 <= v_abs_out0(7);
  s_0025 <= v_abs_out0(8);
  s_0026 <= v_abs_out0(9);
  s_0027 <= v_abs_out0(10);
  s_0028 <= v_abs_out0(11);
  s_0029 <= v_abs_out0(12);
  s_0030 <= v_abs_out0(13);
  s_0031 <= v_abs_out0(14);
  s_0032 <= v_abs_out0(15);
  v_abs_out0 <= std_logic_vector'('0' & s_0014 & s_0013 & s_0012 & s_0011 & s_0010 & s_0009 & s_0008 & s_0007 & s_0006 & s_0005 & s_0004 & s_0003 & s_0002 & s_0001 & s_0000);
  s_0016 <= v_abs_out1(0);
  v_abs_out1 <= std_logic_vector'(0 => (s_0010 and s_0011 and s_0012 and s_0013 and s_0014) and (s_0000 or s_0001 or s_0002 or s_0003 or s_0004 or s_0005 or s_0006 or s_0007 or s_0008 or s_0009));
end architecture;
