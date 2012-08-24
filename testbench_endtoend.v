module testbench_endtoend();

   reg clk;
   reg xreset;
   reg rs_rx;
   wire rs_tx;

   top dut (.CLK(clk), .XRST(xreset), .RS_RX(rs_rx), .RS_TX(rs_tx));

   // initialize test by xresetting
   initial begin
      xreset <= 0;
      rs_rx  <= 1;
      #22;
      xreset <= 1;
      #260;

      rs_rx <= 0;
      #2000;
      rs_rx <= 1;
      #2000;
      rs_rx <= 0;
      #2000;
      rs_rx <= 0;
      #2000;
      rs_rx <= 1;
      #2000;
      rs_rx <= 0;
      #2000;
      rs_rx <= 1;
      #2000;
      rs_rx <= 1;
      #2000;
      rs_rx <= 0;
      #2000;
      rs_rx <= 1;
   end

   // geenrate clock to sequence tests
   always begin
      clk <= 1;
      #7;
      clk <= 0;
      #7;
   end

endmodule
