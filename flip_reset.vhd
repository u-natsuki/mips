library IEEE;
use IEEE.STD_LOGIC_1164.all;

entity flip_reset is
  
  generic (
    width : integer := 32);

  port (
    clk, reset : in  std_logic;
    d          : in  std_logic_vector(width-1 downto 0);
    q          : out std_logic_vector(width-1 downto 0));

end flip_reset;

architecture asynchronous of flip_reset is

begin  -- asynchronous

  flip: process (clk, reset)
  begin  -- process flip
    if reset = '1' then
      q <=  conv_std_logic_vector(0, width);
    elsif clk'event and clk = '1' then
      q <= d;
    end if;
  end process flip;

end asynchronous;
