`timescale 1ns/1ps
module tb;
  reg [7:0] dataIn;
  wire [7:0] result;
  wire fpError;
  FPABSOLUTE_8 dut(.dataIn(dataIn), .result(result), .fpError(fpError));
  initial begin
    dataIn = 8'b00000000; #1;
    if (result !== 8'b00000000 || fpError !== 1'b0) begin
      $display("FAIL vector 0 %b %b", result, fpError); $finish; end
    dataIn = 8'b10000000; #1;
    if (result !== 8'b00000000 || fpError !== 1'b0) begin
      $display("FAIL vector 1 %b %b", result, fpError); $finish; end
    dataIn = 8'b00000001; #1;
    if (result !== 8'b00000001 || fpError !== 1'b0) begin
      $display("FAIL vector 2 %b %b", result, fpError); $finish; end
    dataIn = 8'b10000001; #1;
    if (result !== 8'b00000001 || fpError !== 1'b0) begin
      $display("FAIL vector 3 %b %b", result, fpError); $finish; end
    dataIn = 8'b00111100; #1;
    if (result !== 8'b00111100 || fpError !== 1'b0) begin
      $display("FAIL vector 4 %b %b", result, fpError); $finish; end
    dataIn = 8'b10111100; #1;
    if (result !== 8'b00111100 || fpError !== 1'b0) begin
      $display("FAIL vector 5 %b %b", result, fpError); $finish; end
    dataIn = 8'b01000000; #1;
    if (result !== 8'b01000000 || fpError !== 1'b0) begin
      $display("FAIL vector 6 %b %b", result, fpError); $finish; end
    dataIn = 8'b11000000; #1;
    if (result !== 8'b01000000 || fpError !== 1'b0) begin
      $display("FAIL vector 7 %b %b", result, fpError); $finish; end
    dataIn = 8'b01110111; #1;
    if (result !== 8'b01110111 || fpError !== 1'b0) begin
      $display("FAIL vector 8 %b %b", result, fpError); $finish; end
    dataIn = 8'b11110111; #1;
    if (result !== 8'b01110111 || fpError !== 1'b0) begin
      $display("FAIL vector 9 %b %b", result, fpError); $finish; end
    dataIn = 8'b01111000; #1;
    if (result !== 8'b01111000 || fpError !== 1'b0) begin
      $display("FAIL vector 10 %b %b", result, fpError); $finish; end
    dataIn = 8'b11111000; #1;
    if (result !== 8'b01111000 || fpError !== 1'b0) begin
      $display("FAIL vector 11 %b %b", result, fpError); $finish; end
    dataIn = 8'b01111101; #1;
    if (result !== 8'b01111101 || fpError !== 1'b1) begin
      $display("FAIL vector 12 %b %b", result, fpError); $finish; end
    dataIn = 8'b11111101; #1;
    if (result !== 8'b01111101 || fpError !== 1'b1) begin
      $display("FAIL vector 13 %b %b", result, fpError); $finish; end
    dataIn = 8'b01111001; #1;
    if (result !== 8'b01111001 || fpError !== 1'b1) begin
      $display("FAIL vector 14 %b %b", result, fpError); $finish; end
    dataIn = 8'b11111001; #1;
    if (result !== 8'b01111001 || fpError !== 1'b1) begin
      $display("FAIL vector 15 %b %b", result, fpError); $finish; end
    $display("FP_ABSOLUTE_PASS FPABSOLUTE_8");
    $finish;
  end
endmodule
