library IEEE;
use IEEE.STD_LOGIC_1164.all;
use IEEE.STD_LOGIC_UNSIGNED.all;

library UNISIM;
use UNISIM.VComponents.all;

entity top is
  Port (
    ZD     : inout std_logic_vector(31 downto 0);
    ZDP    : inout std_logic_vector(3 downto 0);

    ZA     : out std_logic_vector(19 downto 0);
    XE1    : out std_logic;
    E2A    : out std_logic;
    XE3    : out std_logic;
    XZBE   : out std_logic_vector(3 downto 0);
    XGA    : out std_logic;
    XWA    : out std_logic;
    XZCKE  : out std_logic;
    ZCLKMA : out std_logic_vector(1 downto 0);

    ADVA   : out std_logic;
    XFT    : out std_logic;
    XLBO   : out std_logic;
    ZZA    : out std_logic;

    CLK    : in STD_LOGIC;
    XRST   : in STD_LOGIC;
    RS_RX  : in STD_LOGIC;
    RS_TX  : out  STD_LOGIC);
end top;

architecture top of top is

  component mips
    port (
      clk, reset          : in  STD_LOGIC;
      pc                  : out STD_LOGIC_VECTOR(31 downto 0);
      instruction         : in  STD_LOGIC_VECTOR(31 downto 0);
      mem_write           : out STD_LOGIC;
      send_enable         : out STD_LOGIC;
      alu_out, write_data : out STD_LOGIC_VECTOR(31 downto 0);
      data_from_bus       : in  STD_LOGIC_VECTOR(31 downto 0);
      rx_enable           : out STD_LOGIC;
      rx_done             : in  STD_LOGIC);
  end component;

  component instruction_memory
     port (
       a  : in  std_logic_vector(5 downto 0);
       rd : out std_logic_vector(31 downto 0));
  end component;

  component real_data_memory is
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
      write_flag : in std_logic
      );
  end component;

  component rs232c_buffer is
    generic (wtime : std_logic_vector(15 downto 0) := x"008F");

    port (
      clk       : in std_logic;
      reset     : in std_logic;
      push      : in std_logic;           -- 1 to push data
      push_data : in std_logic_vector(31 downto 0);
      tx        : out std_logic);

  end component;

  component i232c
    port ( clk    : in  STD_LOGIC;
           enable : in  STD_LOGIC;
           rx     : in  STD_LOGIC;
           data   : out STD_LOGIC_VECTOR (7 downto 0);
           changed: out STD_LOGIC);
  end component;

  signal pc, instruction, data_from_bus, memory_data : std_logic_vector(31 downto 0);

  signal write_data, data_addr : std_logic_vector(31 downto 0);
  signal mem_write : STD_LOGIC;

  signal rx_data : std_logic_vector(7 downto 0);

  signal mclk, iclk : std_logic;

  signal rx_enable, rx_done, send_enable : STD_LOGIC;

begin  -- test

  ib: IBUFG port map (
    i=>CLK,
    o=>mclk);
  bg: BUFG port map (
    i=>mclk,
    o=>iclk);

  imem1 : instruction_memory port map(pc(7 downto 2), instruction);

  mips1 : mips port map (
    clk           => iclk,
    reset         => not xrst,
    pc            => pc,
    instruction   => instruction,
    mem_write     => mem_write,
    send_enable   => send_enable,
    alu_out       => data_addr,
    write_data    => write_data,
    data_from_bus => data_from_bus,
    rx_enable     => rx_enable,
    rx_done       => rx_done);

  receiver : i232c port map (
    clk     => iclk,
    enable  => rx_enable,
    rx      => RS_RX,
    data    => rx_data,
    changed => rx_done);

  sender : rs232c_buffer port map (
    clk       => iclk,
    reset     => not xrst,
    push      => send_enable,
    push_data => write_data,
    tx        => RS_TX);

  data_memory : real_data_memory port map (
    ZD         => ZD,
    ZDP        => ZDP,
    ZA         => ZA,
    XZBE       => XZBE,
    XWA        => XWA,
    clk        => iclk,
    data_read  => memory_data,
    data_write => write_data,
    address    => data_addr,
    write_flag => mem_write);
  
  -- is this good design to judge here?
  -- ok for reading twice?
  data_from_bus <= x"000000" & rx_data when rx_enable = '1' or rx_done = '1' else memory_data;

  -- static SRAM signals
  XE1 <= '0';
  E2A <= '1';
  XE3 <= '0';
  XGA <= '0';
  XZCKE <= '0';
  ZCLKMA(0) <= iclk;
  ZCLKMA(1) <= iclk;
  ADVA <= '0';
  XFT <= '1';
  XLBO <= '1';
  ZZA <= '0';

end top;
