module FPABSOLUTE_8(dataIn, result, fpError);
  input [7:0] dataIn;
  output [7:0] result;
  output fpError;
localparam [8-1:0] NAN_V = {1'b0, {4{1'b1}}, 1'b1, {(3-1){1'b0}}};

function [8-1:0] fp_inf;
   input s;
   begin
      fp_inf = {s, {4{1'b1}}, {3{1'b0}}};
   end
endfunction

function [8-1:0] fp_zero;
   input s;
   begin
      fp_zero = {s, {(8-1){1'b0}}};
   end
endfunction

function fp_isnan;
   input [8-1:0] x;
   begin
      fp_isnan = (x[8-2:3] == 15) && (x[3-1:0] != 0);
   end
endfunction

function fp_isinf;
   input [8-1:0] x;
   begin
      fp_isinf = (x[8-2:3] == 15) && (x[3-1:0] == 0);
   end
endfunction

function fp_iszero;
   input [8-1:0] x;
   begin
      fp_iszero = (x[8-2:0] == 0);
   end
endfunction

function [3:0] fp_sig;
   input [8-1:0] x;
   reg [3-1:0] f;
   reg [3:0] r;
   integer lz, i;
   begin
      f = x[3-1:0];
      if (x[8-2:3] != 0) r = {1'b1, f};
      else begin
         lz = 3;
         for (i = 0; i < 3; i = i + 1)
            if (f[i]) lz = 3 - 1 - i;
         r = {1'b0, f} << (lz + 1);
      end
      fp_sig = r;
   end
endfunction

function integer fp_exp;
   input [8-1:0] x;
   integer ex, lz, i;
   begin
      ex = x[8-2:3];
      if (ex != 0) fp_exp = ex - 7;
      else begin
         lz = 3;
         for (i = 0; i < 3; i = i + 1)
            if (x[i]) lz = 3 - 1 - i;
         fp_exp = 1 - 7 - lz - 1;
      end
   end
endfunction

assign result  = {1'b0, dataIn[8-2:0]};
assign fpError = fp_isnan(dataIn);


endmodule
