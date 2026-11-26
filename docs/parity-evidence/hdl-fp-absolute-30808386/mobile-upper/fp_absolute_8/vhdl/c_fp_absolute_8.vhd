-- Generated deterministically from circuit fp_absolute_8.
-- VHDL-2008. Defined binary inputs; simulator delays are not represented.
LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
USE ieee.numeric_std.ALL;

ENTITY c_fp_absolute_8 IS
  PORT (
    p_input_0 : IN std_logic_vector(7 DOWNTO 0);
    p_output_0 : OUT std_logic_vector(7 DOWNTO 0);
    p_output_1 : OUT std_logic_vector(0 DOWNTO 0)
  );
END ENTITY;

ARCHITECTURE rtl OF c_fp_absolute_8 IS
  FUNCTION hdl_choose(condition : boolean; yes_value, no_value : std_logic_vector) RETURN std_logic_vector IS
  BEGIN
    IF condition THEN RETURN yes_value; ELSE RETURN no_value; END IF;
  END FUNCTION;
  SIGNAL s_0000 : std_logic;
  SIGNAL s_0001 : std_logic;
  SIGNAL s_0002 : std_logic;
  SIGNAL s_0003 : std_logic;
  SIGNAL s_0004 : std_logic;
  SIGNAL s_0005 : std_logic;
  SIGNAL s_0006 : std_logic;
  SIGNAL s_0007 : std_logic;
  SIGNAL s_0008 : std_logic;
  SIGNAL s_0009 : std_logic;
  SIGNAL s_0010 : std_logic;
  SIGNAL s_0011 : std_logic;
  SIGNAL s_0012 : std_logic;
  SIGNAL s_0013 : std_logic;
  SIGNAL s_0014 : std_logic;
  SIGNAL s_0015 : std_logic;
  SIGNAL s_0016 : std_logic;
  SIGNAL v_abs_out0 : std_logic_vector(7 DOWNTO 0);
  SIGNAL v_abs_out1 : std_logic_vector(0 DOWNTO 0);
BEGIN
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
  v_abs_out1 <= std_logic_vector'(0 => (s_0003 AND s_0004 AND s_0005 AND s_0006) AND (s_0000 OR s_0001 OR s_0002));
END ARCHITECTURE;
