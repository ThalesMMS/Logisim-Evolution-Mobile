module FPABSOLUTE_32(dataIn, result, fpError);
  input [31:0] dataIn;
  output [31:0] result;
  output fpError;
localparam [32-1:0] NAN_V = {1'b0, {8{1'b1}}, 1'b1, {(23-1){1'b0}}};

function [32-1:0] fp_inf;
   input s;
   begin
      fp_inf = {s, {8{1'b1}}, {23{1'b0}}};
   end
endfunction

function [32-1:0] fp_zero;
   input s;
   begin
      fp_zero = {s, {(32-1){1'b0}}};
   end
endfunction

function fp_isnan;
   input [32-1:0] x;
   begin
      fp_isnan = (x[32-2:23] == 255) && (x[23-1:0] != 0);
   end
endfunction

function fp_isinf;
   input [32-1:0] x;
   begin
      fp_isinf = (x[32-2:23] == 255) && (x[23-1:0] == 0);
   end
endfunction

function fp_iszero;
   input [32-1:0] x;
   begin
      fp_iszero = (x[32-2:0] == 0);
   end
endfunction

function [23:0] fp_sig;
   input [32-1:0] x;
   reg [23-1:0] f;
   reg [23:0] r;
   integer lz, i;
   begin
      f = x[23-1:0];
      if (x[32-2:23] != 0) r = {1'b1, f};
      else begin
         lz = 23;
         for (i = 0; i < 23; i = i + 1)
            if (f[i]) lz = 23 - 1 - i;
         r = {1'b0, f} << (lz + 1);
      end
      fp_sig = r;
   end
endfunction

function integer fp_exp;
   input [32-1:0] x;
   integer ex, lz, i;
   begin
      ex = x[32-2:23];
      if (ex != 0) fp_exp = ex - 127;
      else begin
         lz = 23;
         for (i = 0; i < 23; i = i + 1)
            if (x[i]) lz = 23 - 1 - i;
         fp_exp = 1 - 127 - lz - 1;
      end
   end
endfunction

assign result  = {1'b0, dataIn[32-2:0]};
assign fpError = fp_isnan(dataIn);


endmodule
