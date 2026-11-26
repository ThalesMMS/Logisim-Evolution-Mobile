`timescale 1ns/1ps
module tb;
  reg [15:0] dataIn;
  wire [15:0] result;
  wire fpError;
  FPABSOLUTE_16 dut(.dataIn(dataIn), .result(result), .fpError(fpError));
  initial begin
    dataIn = 16'b0000000000000000; #1;
    if (result !== 16'b0000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 0 %b %b", result, fpError); $finish; end
    dataIn = 16'b1000000000000000; #1;
    if (result !== 16'b0000000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 1 %b %b", result, fpError); $finish; end
    dataIn = 16'b0000000000000001; #1;
    if (result !== 16'b0000000000000001 || fpError !== 1'b0) begin
      $display("FAIL vector 2 %b %b", result, fpError); $finish; end
    dataIn = 16'b1000000000000001; #1;
    if (result !== 16'b0000000000000001 || fpError !== 1'b0) begin
      $display("FAIL vector 3 %b %b", result, fpError); $finish; end
    dataIn = 16'b0011111000000000; #1;
    if (result !== 16'b0011111000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 4 %b %b", result, fpError); $finish; end
    dataIn = 16'b1011111000000000; #1;
    if (result !== 16'b0011111000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 5 %b %b", result, fpError); $finish; end
    dataIn = 16'b0100000000000000; #1;
    if (result !== 16'b0100000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 6 %b %b", result, fpError); $finish; end
    dataIn = 16'b1100000000000000; #1;
    if (result !== 16'b0100000000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 7 %b %b", result, fpError); $finish; end
    dataIn = 16'b0111101111111111; #1;
    if (result !== 16'b0111101111111111 || fpError !== 1'b0) begin
      $display("FAIL vector 8 %b %b", result, fpError); $finish; end
    dataIn = 16'b1111101111111111; #1;
    if (result !== 16'b0111101111111111 || fpError !== 1'b0) begin
      $display("FAIL vector 9 %b %b", result, fpError); $finish; end
    dataIn = 16'b0111110000000000; #1;
    if (result !== 16'b0111110000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 10 %b %b", result, fpError); $finish; end
    dataIn = 16'b1111110000000000; #1;
    if (result !== 16'b0111110000000000 || fpError !== 1'b0) begin
      $display("FAIL vector 11 %b %b", result, fpError); $finish; end
    dataIn = 16'b0111111000000001; #1;
    if (result !== 16'b0111111000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 12 %b %b", result, fpError); $finish; end
    dataIn = 16'b1111111000000001; #1;
    if (result !== 16'b0111111000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 13 %b %b", result, fpError); $finish; end
    dataIn = 16'b0111110000000001; #1;
    if (result !== 16'b0111110000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 14 %b %b", result, fpError); $finish; end
    dataIn = 16'b1111110000000001; #1;
    if (result !== 16'b0111110000000001 || fpError !== 1'b1) begin
      $display("FAIL vector 15 %b %b", result, fpError); $finish; end
    $display("FP_ABSOLUTE_PASS FPABSOLUTE_16");
    $finish;
  end
endmodule
