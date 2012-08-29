library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_ARITH.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;
use IEEE.STD_LOGIC_MISC.ALL;

-- Constant lines should be trimmed on top entity.
entity real_data_memory is
  Port (
    ZD         : inout std_logic_vector(31 downto 0);
    ZDP        : inout std_logic_vector(3 downto 0);

    ZA         : out std_logic_vector(19 downto 0);
    XZBE       : out std_logic_vector(3 downto 0);
    XWA        : out std_logic;

    clk        : in std_logic;
    data_read  : out std_logic_vector(31 downto 0);
    data_write : in std_logic_vector(31 downto 0);
    address    : in std_logic_vector(19 downto 0);
    bit_enable : in std_logic_vector(3 downto 0);
    write_flag : in std_logic
    );
end sramc;

architecture blackbox of sramc is
  signal wstate : std_logic_vector(2 downto 0) := "000";
  signal parity_write : std_logic_vector(3 downto 0) := "0000";
begin  -- blackbox

  address_latch: process (clk, address, write_flag)
  begin
    if rising_edge(clk) then
      ZA <= address;
      if write_flag = '1' and wstate /= "000" then
        XWA <= '0';
        ZD <= data_write;
        ZDP <= parity_write;
        XZBE <= not bit_enable;
      else
        XWA <= '1';
        ZD <= (others => 'Z');
        ZDP <= (others => 'Z');
        XZBE <= (others => '0');
      end if;
    end if;
  end process address_latch;

  read_latch: process (clk, ZD)
  begin  -- process read_latch
    if rising_edge(clk) then
      if write_flag = '0' then
        data_read <= ZD;
      end if;
    end if;
  end process read_latch;

  -- purpose: send data to SRAM
  -- type   : combinational
  -- inputs : clk, write_flag
  -- outputs: ZD, ZDP, XWA
  write: process (clk, write_flag)
  begin  -- process write
    if rising_edge(clk) then
      if write_flag = '1' and wstate = "000" then
        wstate <= "001";
      end if;
      if wstate = "100" then
        wstate <= "000";
      elsif wstate /= "000" then
        wstate <= wstate + 1;
      end if;
    end if;
  end process write;

  parity_write(0) <= xor_reduce(data_write( 7 downto  0));
  parity_write(1) <= xor_reduce(data_write(15 downto  8));
  parity_write(2) <= xor_reduce(data_write(23 downto 16));
  parity_write(3) <= xor_reduce(data_write(31 downto 24));

end blackbox;
