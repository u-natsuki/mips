-- from VHDL coding tips and tricks.  written by Vipin
-- http://vhdlguru.blogspot.jp/2011/01/implementation-of-stack-in-vhdl.html

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_ARITH.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;
use IEEE.STD_LOGIC_MISC.ALL;

entity stack is
  generic (
    width : integer := 32
    );
  port(   Clk         : in std_logic;  --Clock for the stack.
          Enable      : in std_logic;  --Enable the stack. Otherwise neither push nor pop will happen.
          Data_In     : in std_logic_vector(width-1 downto 0);  --Data to be pushed to stack
          Data_Out    : out std_logic_vector(width-1 downto 0);  --Data popped from the stack.
          PUSH_barPOP : in std_logic;  --active low for POP and active high for PUSH.
          Stack_Full  : out std_logic;  --Goes high when the stack is full.
          Stack_Empty : out std_logic  --Goes high when the stack is empty.
          );
end stack;

architecture Behavioral of stack is

  -- 0 is not accessible
  type mem_type is array (255 downto 1) of std_logic_vector(width-1 downto 0);
  signal stack_mem : mem_type := (others => (others => '0'));
  signal stack_ptr : std_logic_vector(7 downto 0) := x"ff";
  signal full,empty : std_logic := '0';

begin

  Stack_Full <= full;
  Stack_Empty <= empty;

  process (stack_ptr, stack_mem)
  begin
    --setting full and empty flags
    if(stack_ptr = x"00") then
      full <= '1';
      empty <= '0';
    elsif(stack_ptr = x"ff") then
      full <= '0';
      empty <= '1';
    else
      full <= '0';
      empty <= '0';
    end if;

    if (stack_ptr = x"ff") then
      Data_Out <= (others => 'X');
    else
      Data_Out <= stack_mem(conv_integer(stack_ptr+1));
    end if;
  end process;

  --PUSH and POP process for the stack.
  PUSH : process(Clk,PUSH_barPOP,Enable)
  begin
    if(rising_edge(Clk)) then

      --PUSH section.
      if (Enable = '1' and PUSH_barPOP = '1' and full = '0') then
        --Data pushed to the current address.
        stack_mem(conv_integer(stack_ptr)) <= Data_In;
        if(stack_ptr /= 0) then
          stack_ptr <= stack_ptr - 1;
        end if;
      end if;
      --POP section.
      if (Enable = '1' and PUSH_barPOP = '0' and empty = '0') then
        --Data has to be taken from the next highest address(empty descending type stack).
        if(stack_ptr /= x"ff") then
          stack_ptr <= stack_ptr + 1;
        end if;
      end if;

    end if;
  end process;

end Behavioral;
