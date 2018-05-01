/* Author: Rui Pedro Paiva
Teoria da Informação, LEI, 2006/2007*/
package gzip;
import java.io.*;

//classe com os campos referentes ao cabeçalho de um ficheiro gzip
//uma só função para ler o cabeçalho
public class gzipHeader {
	//elementos fixos
	byte ID1, ID2, CM, FLG, XFL, OS;
	byte [] MTIME;
	final short lenMTIME = 4;
	long mTime = 0;
	byte FLG_FTEXT, FLG_FHCRC, FLG_FEXTRA, FLG_FNAME, FLG_FCOMMENT;   //bits 0, 1, 2, 3 e 4, respectivamente (restantes 3: reservados)
	
	// FLG_FTEXT --> ignorado deliberadamente (tipicamente igual a 0)
	//se FLG_FEXTRA == 1
	byte [] XLEN, extraField;
	final byte lenXLEN = 2;
	
	//se FLG_FNAME == 1
	String fName = "";  //terminada por um byte a 0
	
	//se FLG_FCOMMENT == 1
	String fComment; //terminada por um byte a 0
		
	//se FLG_HCRC == 1
	byte [] HCRC;
	final byte lenHCRC = 2;
		
		
	
	// ler cabeçalho: devolve -1 se erro, erro se ok	
	int read(RandomAccessFile is) throws IOException
	{
		//Identicação 1 e 2: valores fixos
		ID1 = is.readByte();  
		if (ID1 != 0x1f) return -1; //erro no cabeçalho
			
		ID2 = is.readByte();
		if (ID2 != (byte)(0x8b)) return -1; //erro no cabeçalho
		
		//Método de compressão (deve ser 8 para denotar o deflate)
		CM = is.readByte();  		
		if (CM != 0x08) return -1; //erro no cabeçalho
					
		//Flags
		FLG = is.readByte();
		
		//MTIME
		MTIME = new byte[lenMTIME];
		mTime = MTIME[0] = is.readByte();
		for (int i = 1; i <= lenMTIME - 1; i++)
		{
			MTIME[i] = is.readByte();
			mTime = MTIME[i] << 8 + mTime;				
		}
						
		//XFL (not processed...)
		XFL = is.readByte();
		
		//OS (not processed...)
		OS = is.readByte();		
		
		//--- Check Flags
		FLG_FTEXT = (byte)(FLG & 0x01);
		FLG_FHCRC = (byte)((FLG & 0x02) >> 1);
		FLG_FEXTRA = (byte)((FLG & 0x04) >> 2);
		FLG_FNAME = (byte)((FLG & 0x08) >> 3);
		FLG_FCOMMENT = (byte)((FLG & 0x10) >> 4);
					
		//FLG_EXTRA
		if (FLG_FEXTRA == 1)
		{
			//ler 2 bytes XLEN + XLEN bytes de extra field
			//1º byte: LSB, 2º: MSB
			XLEN = new byte[lenXLEN];
			XLEN[0] = is.readByte();
			XLEN[1] = is.readByte();
			int xlen = XLEN[1] << 8 + XLEN[0];
			
			extraField = new byte[xlen];
			
			//ler extra field (deixado como está, i.e., não processado...)
			for (int i = 0; i <= xlen - 1; i++)
				extraField[i] = is.readByte();
		}
		
		//FLG_FNAME
		if (FLG_FNAME == 1)
		{			
			char c;
			do
			{
				c = (char) is.readByte();
				if (c != 0)
					fName += c;
			}while(c != 0);
		}
		
		//FLG_FCOMMENT
		if (FLG_FCOMMENT == 1)
		{			
			char c;
			do
			{
				c = (char) is.readByte();
				if (c != 0)
					fComment += c;
			}while(c != 0);
		}
		
		//FLG_FHCRC (not processed...)
		if (FLG_FHCRC == 1)
		{			
			HCRC = new byte[lenHCRC];
			HCRC[0] = is.readByte();
			HCRC[1] = is.readByte();				
		}			
		
		return 0;
	}		
}