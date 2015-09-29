package framing;

import java.util.Arrays;

import packets.PacketException;

/** This is COBS encoding, the data is read into segments with 0x00 as delimiters, these 0x00s are removed and the length of each sengment + 1
 * is prepended onto the front. 0xFF has a special meaning, it represents a block of length of 254 */
public class COBS implements FramingAlgorithm {
	
	@Override
	public byte[] encode(byte[] data) {
		byte[] n = new byte[data.length + data.length / 254 + 2];
		
		int outPos = 1;
		int codePos = 0;
		byte code = 0x01;
		
		for(byte b : data) {
			if(b == 0) {
				n[codePos] = code;
				code = 0x01;
				codePos = outPos++;
			} else {
				code++;
				n[outPos++] = b;
				
				if(code == -1) { //0xff as a signed byte
					n[codePos] = code;
					code = 0x01;
					codePos = outPos++;
				}
			}
		}
		
		n[codePos] = code;
		
		return Arrays.copyOf(n, outPos);
	}

	@Override
	public byte[] decode(byte[] data) throws PacketException {
		if(data.length == 0) {
			throw new PacketException(-1, "The COBS sequence is malformed");
		}
		
		byte[] newData = new byte[data.length];
		
		int in = 0;
		int out = 0;
		
		while(true) {
			byte code = data[in++];
			
			for(int j = 0; j < code - 1; j++) {
				newData[out++] = data[in++];
			}
			
			if(in == data.length)
				break;
			
			if(code != 0xFF)
				newData[out++] = 0x00;
		}
		
		return Arrays.copyOf(newData, out);
	}
}
