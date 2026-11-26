library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity FPABSOLUTE_64 is
  port (dataIn : in std_logic_vector(63 downto 0);
        result : out std_logic_vector(63 downto 0);
        fpError : out std_logic);
end entity;

architecture platformIndependent of FPABSOLUTE_64 is
begin
fpcore : block is
   function fp_nan return std_logic_vector is
      variable r : std_logic_vector(64-1 downto 0) := (others => '0');
   begin
      r(64-2 downto 52) := (others => '1');
      r(52-1) := '1';
      return r;
   end function;

   function fp_inf(s : std_logic) return std_logic_vector is
      variable r : std_logic_vector(64-1 downto 0) := (others => '0');
   begin
      r(64-1) := s;
      r(64-2 downto 52) := (others => '1');
      return r;
   end function;

   function fp_zero(s : std_logic) return std_logic_vector is
      variable r : std_logic_vector(64-1 downto 0) := (others => '0');
   begin
      r(64-1) := s;
      return r;
   end function;

   function fp_isnan(x : std_logic_vector(64-1 downto 0)) return boolean is
   begin
      return unsigned(x(64-2 downto 52)) = 2047 and unsigned(x(52-1 downto 0)) /= 0;
   end function;

   function fp_isinf(x : std_logic_vector(64-1 downto 0)) return boolean is
   begin
      return unsigned(x(64-2 downto 52)) = 2047 and unsigned(x(52-1 downto 0)) = 0;
   end function;

   function fp_iszero(x : std_logic_vector(64-1 downto 0)) return boolean is
   begin
      return unsigned(x(64-2 downto 0)) = 0;
   end function;

   function fp_sig(x : std_logic_vector(64-1 downto 0)) return unsigned is
      variable f  : unsigned(52-1 downto 0);
      variable lz : integer;
      variable r  : unsigned(52 downto 0);
   begin
      f := unsigned(x(52-1 downto 0));
      if unsigned(x(64-2 downto 52)) /= 0 then
         r := '1' & f;
      else
         lz := 52;
         for i in 0 to 52-1 loop
            if f(i) = '1' then
               lz := 52-1-i;
            end if;
         end loop;
         r := shift_left('0' & f, lz + 1);
      end if;
      return r;
   end function;

   function fp_exp(x : std_logic_vector(64-1 downto 0)) return integer is
      variable f  : unsigned(52-1 downto 0);
      variable ex : integer;
      variable lz : integer;
   begin
      f  := unsigned(x(52-1 downto 0));
      ex := to_integer(unsigned(x(64-2 downto 52)));
      if ex /= 0 then
         return ex - 1023;
      end if;
      lz := 52;
      for i in 0 to 52-1 loop
         if f(i) = '1' then
            lz := 52-1-i;
         end if;
      end loop;
      return 1 - 1023 - lz - 1;
   end function;

begin
   result  <= '0' & dataIn(64-2 downto 0);
   fpError <= '1' when fp_isnan(dataIn) else '0';

end block fpcore;

end architecture;
