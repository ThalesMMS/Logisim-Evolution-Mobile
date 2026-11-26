// Generated deterministically from circuit fp_absolute_8.
// Defined binary inputs; simulator propagation delays are not represented.
`default_nettype none
module c_fp_absolute_8(p_input_0, p_output_0, p_output_1);
  input wire [7:0] p_input_0;
  output wire [7:0] p_output_0;
  output wire [0:0] p_output_1;
  wire s_0000;
  wire s_0001;
  wire s_0002;
  wire s_0003;
  wire s_0004;
  wire s_0005;
  wire s_0006;
  wire s_0007;
  wire s_0008;
  wire s_0009;
  wire s_0010;
  wire s_0011;
  wire s_0012;
  wire s_0013;
  wire s_0014;
  wire s_0015;
  wire s_0016;
  wire [7:0] v_abs_out0;
  wire [0:0] v_abs_out1;
  assign s_0000 = p_input_0[0];
  assign s_0001 = p_input_0[1];
  assign s_0002 = p_input_0[2];
  assign s_0003 = p_input_0[3];
  assign s_0004 = p_input_0[4];
  assign s_0005 = p_input_0[5];
  assign s_0006 = p_input_0[6];
  assign s_0007 = p_input_0[7];
  assign p_output_0[0] = s_0009;
  assign p_output_0[1] = s_0010;
  assign p_output_0[2] = s_0011;
  assign p_output_0[3] = s_0012;
  assign p_output_0[4] = s_0013;
  assign p_output_0[5] = s_0014;
  assign p_output_0[6] = s_0015;
  assign p_output_0[7] = s_0016;
  assign p_output_1[0] = s_0008;
  assign s_0009 = v_abs_out0[0];
  assign s_0010 = v_abs_out0[1];
  assign s_0011 = v_abs_out0[2];
  assign s_0012 = v_abs_out0[3];
  assign s_0013 = v_abs_out0[4];
  assign s_0014 = v_abs_out0[5];
  assign s_0015 = v_abs_out0[6];
  assign s_0016 = v_abs_out0[7];
  assign v_abs_out0 = {1'b0, s_0006, s_0005, s_0004, s_0003, s_0002, s_0001, s_0000};
  assign s_0008 = v_abs_out1[0];
  assign v_abs_out1 = {(s_0003 & s_0004 & s_0005 & s_0006) & (s_0000 | s_0001 | s_0002)};
endmodule
`default_nettype wire
