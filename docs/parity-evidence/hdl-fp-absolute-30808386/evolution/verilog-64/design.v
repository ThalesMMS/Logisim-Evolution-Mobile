module FPABSOLUTE_64(dataIn, result, fpError);
  input [63:0] dataIn;
  output [63:0] result;
  output fpError;
localparam [64-1:0] NAN_V = {1'b0, {11{1'b1}}, 1'b1, {(52-1){1'b0}}};

function [64-1:0] fp_inf;
   input s;
   begin
      fp_inf = {s, {11{1'b1}}, {52{1'b0}}};
   end
endfunction

function [64-1:0] fp_zero;
   input s;
   begin
      fp_zero = {s, {(64-1){1'b0}}};
   end
endfunction

function fp_isnan;
   input [64-1:0] x;
   begin
      fp_isnan = (x[64-2:52] == 2047) && (x[52-1:0] != 0);
   end
endfunction

function fp_isinf;
   input [64-1:0] x;
   begin
      fp_isinf = (x[64-2:52] == 2047) && (x[52-1:0] == 0);
   end
endfunction

function fp_iszero;
   input [64-1:0] x;
   begin
      fp_iszero = (x[64-2:0] == 0);
   end
endfunction

function [52:0] fp_sig;
   input [64-1:0] x;
   reg [52-1:0] f;
   reg [52:0] r;
   integer lz, i;
   begin
      f = x[52-1:0];
      if (x[64-2:52] != 0) r = {1'b1, f};
      else begin
         lz = 52;
         for (i = 0; i < 52; i = i + 1)
            if (f[i]) lz = 52 - 1 - i;
         r = {1'b0, f} << (lz + 1);
      end
      fp_sig = r;
   end
endfunction

function integer fp_exp;
   input [64-1:0] x;
   integer ex, lz, i;
   begin
      ex = x[64-2:52];
      if (ex != 0) fp_exp = ex - 1023;
      else begin
         lz = 52;
         for (i = 0; i < 52; i = i + 1)
            if (x[i]) lz = 52 - 1 - i;
         fp_exp = 1 - 1023 - lz - 1;
      end
   end
endfunction

assign result  = {1'b0, dataIn[64-2:0]};
assign fpError = fp_isnan(dataIn);


endmodule
