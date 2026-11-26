module FPABSOLUTE_16(dataIn, result, fpError);
  input [15:0] dataIn;
  output [15:0] result;
  output fpError;
localparam [16-1:0] NAN_V = {1'b0, {5{1'b1}}, 1'b1, {(10-1){1'b0}}};

function [16-1:0] fp_inf;
   input s;
   begin
      fp_inf = {s, {5{1'b1}}, {10{1'b0}}};
   end
endfunction

function [16-1:0] fp_zero;
   input s;
   begin
      fp_zero = {s, {(16-1){1'b0}}};
   end
endfunction

function fp_isnan;
   input [16-1:0] x;
   begin
      fp_isnan = (x[16-2:10] == 31) && (x[10-1:0] != 0);
   end
endfunction

function fp_isinf;
   input [16-1:0] x;
   begin
      fp_isinf = (x[16-2:10] == 31) && (x[10-1:0] == 0);
   end
endfunction

function fp_iszero;
   input [16-1:0] x;
   begin
      fp_iszero = (x[16-2:0] == 0);
   end
endfunction

function [10:0] fp_sig;
   input [16-1:0] x;
   reg [10-1:0] f;
   reg [10:0] r;
   integer lz, i;
   begin
      f = x[10-1:0];
      if (x[16-2:10] != 0) r = {1'b1, f};
      else begin
         lz = 10;
         for (i = 0; i < 10; i = i + 1)
            if (f[i]) lz = 10 - 1 - i;
         r = {1'b0, f} << (lz + 1);
      end
      fp_sig = r;
   end
endfunction

function integer fp_exp;
   input [16-1:0] x;
   integer ex, lz, i;
   begin
      ex = x[16-2:10];
      if (ex != 0) fp_exp = ex - 15;
      else begin
         lz = 10;
         for (i = 0; i < 10; i = i + 1)
            if (x[i]) lz = 10 - 1 - i;
         fp_exp = 1 - 15 - lz - 1;
      end
   end
endfunction

assign result  = {1'b0, dataIn[16-2:0]};
assign fpError = fp_isnan(dataIn);


endmodule
