`timescale 1ns/1ps
module tb;
  reg [31:0] dataIn;
  wire [31:0] result;
  wire fpError;
  FPABSOLUTE_32 dut(.dataIn(dataIn), .result(result), .fpError(fpError));
  initial begin
    dataIn = 32'b00000000000000000000000000000000; #1;
    if (result !== 32'b00000000000000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 0 %b %b", result, fpError); $finish; end
    dataIn = 32'b10000000000000000000000000000000; #1;
    if (result !== 32'b00000000000000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 1 %b %b", result, fpError); $finish; end
    dataIn = 32'b00000000000000000000000000000001; #1;
    if (result !== 32'b00000000000000000000000000000001 || fpError !== 1'b0) begin
      $display("FAIL vector 2 %b %b", result, fpError); $finish; end
    dataIn = 32'b10000000000000000000000000000001; #1;
    if (result !== 32'b00000000000000000000000000000001 || fpError !== 1'b0) begin
      $display("FAIL vector 3 %b %b", result, fpError); $finish; end
    dataIn = 32'b00111111110000000000000000000000; #1;
    if (result !== 32'b00111111110000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 4 %b %b", result, fpError); $finish; end
    dataIn = 32'b10111111110000000000000000000000; #1;
    if (result !== 32'b00111111110000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 5 %b %b", result, fpError); $finish; end
    dataIn = 32'b01000000000000000000000000000000; #1;
    if (result !== 32'b01000000000000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 6 %b %b", result, fpError); $finish; end
    dataIn = 32'b11000000000000000000000000000000; #1;
    if (result !== 32'b01000000000000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 7 %b %b", result, fpError); $finish; end
    dataIn = 32'b01111111011111111111111111111111; #1;
    if (result !== 32'b01111111011111111111111111111111 || fpError !== 1'b0) begin
      $display("FAIL vector 8 %b %b", result, fpError); $finish; end
    dataIn = 32'b11111111011111111111111111111111; #1;
    if (result !== 32'b01111111011111111111111111111111 || fpError !== 1'b0) begin
      $display("FAIL vector 9 %b %b", result, fpError); $finish; end
    dataIn = 32'b01111111100000000000000000000000; #1;
    if (result !== 32'b01111111100000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 10 %b %b", result, fpError); $finish; end
    dataIn = 32'b11111111100000000000000000000000; #1;
    if (result !== 32'b01111111100000000000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 11 %b %b", result, fpError); $finish; end
    dataIn = 32'b01111111110000000000000000000001; #1;
    if (result !== 32'b01111111110000000000000000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 12 %b %b", result, fpError); $finish; end
    dataIn = 32'b11111111110000000000000000000001; #1;
    if (result !== 32'b01111111110000000000000000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 13 %b %b", result, fpError); $finish; end
    dataIn = 32'b01111111100000000000000000000001; #1;
    if (result !== 32'b01111111100000000000000000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 14 %b %b", result, fpError); $finish; end
    dataIn = 32'b11111111100000000000000000000001; #1;
    if (result !== 32'b01111111100000000000000000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 15 %b %b", result, fpError); $finish; end
    $display("FP_ABSOLUTE_PASS FPABSOLUTE_32");
    $finish;
  end
endmodule
