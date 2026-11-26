-- Generated deterministically from circuit fp_absolute_8.
-- VHDL-2008. Defined binary inputs; simulator delays are not represented.
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity c_fp_absolute_8 is
  port (
    p_input_0 : in std_logic_vector(7 downto 0);
    p_output_0 : out std_logic_vector(7 downto 0);
    p_output_1 : out std_logic_vector(0 downto 0)
  );
end entity;

architecture rtl of c_fp_absolute_8 is
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
  signal v_abs_out0 : std_logic_vector(7 downto 0);
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
  p_output_0(0) <= s_0009;
  p_output_0(1) <= s_0010;
  p_output_0(2) <= s_0011;
  p_output_0(3) <= s_0012;
  p_output_0(4) <= s_0013;
  p_output_0(5) <= s_0014;
  p_output_0(6) <= s_0015;
  p_output_0(7) <= s_0016;
  p_output_1(0) <= s_0008;
  s_0009 <= v_abs_out0(0);
  s_0010 <= v_abs_out0(1);
  s_0011 <= v_abs_out0(2);
  s_0012 <= v_abs_out0(3);
  s_0013 <= v_abs_out0(4);
  s_0014 <= v_abs_out0(5);
  s_0015 <= v_abs_out0(6);
  s_0016 <= v_abs_out0(7);
  v_abs_out0 <= std_logic_vector'('0' & s_0006 & s_0005 & s_0004 & s_0003 & s_0002 & s_0001 & s_0000);
  s_0008 <= v_abs_out1(0);
  v_abs_out1 <= std_logic_vector'(0 => (s_0003 and s_0004 and s_0005 and s_0006) and (s_0000 or s_0001 or s_0002));
end architecture;
