/* Author: Rui Pedro Paiva
 Teoria da Informa��o, LEI, 2006/2007*/
package gzip;

import java.io.*;
import java.util.*;
import java.lang.Math;

//class principal para leitura de um ficheiro gzip
//M�todos:
//gzip(String fileName) throws IOException --> construtor
//int getHeader() throws IOException --> l� o cabe�alho do ficheiro para um objecto da class gzipHeader
//void closeFiles() throws IOException --> fecha os ficheiros
//String bits2String(byte b) --> converte um byte para uma string com a sua representa��o bin�ria
public class Gzip {

    static gzipHeader gzh;
    static String gzFile;
    static long fileSize;
    static long origFileSize;
    static int numBlocks = 0;
    static RandomAccessFile is;
    static int rb = 0, needBits = 0, availBits = 0;

    //fun��o principal, a qual gere todo o processo de descompacta��o
    public static void main(String args[]) {
		//--- obter ficheiro a descompactar

        String fileName = "FAQ.txt.gz";
        /*if (args.length != 1)
         {
         System.out.println("Linha de comando inv�lida!!!");
         return;
         }
         String fileName = args[0];*/

        //--- processar ficheiro
        try {
            Gzip gz = new Gzip(fileName);
			//System.out.println(fileSize);

            //ler tamanho do ficheiro original e definir Vector com s�mbolos
            origFileSize = getOrigFileSize();
            System.out.println(origFileSize);

            //--- ler cabe�alho
            int erro = getHeader();
            if (erro != 0) {
                System.out.println("Formato inv�lido!!!");
                return;
            }
			//else				
            //	System.out.println(gzh.fName);

            //--- Para todos os blocos encontrados
            int BFINAL, HLIT, HDIST, HCLEN;
            int[] array, huff, huffLitComp, huffDist;

            do {
                //--- ler o block header: primeiro byte depois do cabe�alho do ficheiro
                needBits = 3;
                if (availBits < needBits) {
                    rb = is.readUnsignedByte() << availBits | rb;
                    availBits += 8;
                }

				//obter BFINAL
                //ver se � o �ltimo bloco
                BFINAL = (byte) (rb & 0x01); //primeiro bit � o menos significativo
                System.out.println("BFINAL = " + BFINAL);

                //analisar block header e ver se � huffman din�mico					
                if (!isDynamicHuffman(rb)) //ignorar bloco se n�o for Huffman din�mico
                {
                    continue;
                }

                //descartar os 3 bits correspondentes ao tipo de bloco
                rb = rb >> 3;
                availBits -= 3;

				//--- Se chegou aqui --> compactado com Huffman din�mico --> descompactar
                //adicionar programa...
                HLIT = LeBits(5);
                System.out.println("HLIT " + HLIT);
                HDIST = LeBits(5);
                System.out.println("HDIST " + HDIST);
                HCLEN = LeBits(4);
                System.out.println("HCLEN " + HCLEN);

                array = armazenaArray(HCLEN);
                for (int i = 0; i < HCLEN + 4; i++) {
                    System.out.println(array[i]);
                }

                huff = converteHuffman(array);
                System.out.println("Huffman");
                for (int i = 0; i < huff.length; i++) {
                    System.out.println("Posicao " + i + "-" + bits2String((byte) huff[i], array[i]));
                }
                int[] litComp = literaisComprimentos(huff, array, (HLIT + 257));
                System.out.println("Literais/Comprimento HLIT+257");
                for (int i = 0; i < litComp.length; i++) {
                    System.out.println(litComp[i]);
                }
                System.out.println("Distancias HDIST+1");
                int[] dist = literaisComprimentos(huff, array, (HDIST + 1));
                for (int i = 0; i < dist.length; i++) {
                    System.out.println(dist[i]);
                }

                huffLitComp = converteHuffman(litComp);
                System.out.println("Huffman Literais/Comprimentos");
                for (int i = 0; i < huffLitComp.length; i++) {
                    System.out.println("Posicao " + i + "-" + bits2String((byte) huffLitComp[i], litComp[i]));
                }
                huffDist = converteHuffman(dist);
                System.out.println("Huffman Distancias");
                for (int i = 0; i < huffDist.length; i++) {
                    System.out.println("Posicao " + i + "-" + bits2String((byte) huffDist[i], dist[i]));
                }

                LZ77(huffLitComp, litComp, huffDist, dist);

                //actualizar n�mero de blocos analisados
                numBlocks++;
            } while (BFINAL == 0);

            //termina��es			
            is.close();
            System.out.println("End: " + numBlocks + " bloco(s) analisado(s).");
        } catch (IOException erro) {
            System.out.println("Erro ao usar o ficheiro!!!");
            System.out.println(erro);
        }
    }

    //Construtor: recebe nome do ficheiro a descompactar e cria File Streams
    Gzip(String fileName) throws IOException {
        gzFile = fileName;
        is = new RandomAccessFile(fileName, "r");
        fileSize = is.length();
    }

    //Obt�m tamanho do ficheiro original
    public static long getOrigFileSize() throws IOException {
        //salvaguarda posi��o actual do ficheiro
        long fp = is.getFilePointer();

        //�ltimos 4 bytes = ISIZE;
        is.seek(fileSize - 4);

        //determina ISIZE (s� correcto se cabe em 32 bits)
        long sz = 0;
        sz = is.readUnsignedByte();
        for (int i = 0; i <= 2; i++) {
            sz = (is.readUnsignedByte() << 8 * (i + 1)) + sz;
        }

        //restaura file pointer
        is.seek(fp);

        return sz;
    }

    //L� o cabe�alho do ficheiro gzip: devolve erro se o formato for inv�lido
    public static int getHeader() throws IOException //obt�m cabe�alho
    {
        gzh = new gzipHeader();

        int erro = gzh.read(is);
        if (erro != 0) {
            return erro; //formato inv�lido		
        }
        return 0;
    }

    //Analisa block header e v� se � huffman din�mico
    public static boolean isDynamicHuffman(int k) {
        byte BTYPE = (byte) ((k & 0x06) >> 1);

        if (BTYPE == 0) //--> sem compress�o
        {
            System.out.println("Ignorando bloco: sem compacta��o!!!");
            return false;
        } else if (BTYPE == 1) {
            System.out.println("Ignorando bloco: compactado com Huffman fixo!!!");
            return false;
        } else if (BTYPE == 3) {
            System.out.println("Ignorando bloco: BTYPE = reservado!!!");
            return false;
        } else {
            return true;
        }

    }

    //Converte um byte para uma string com a sua representa��o bin�ria
    public static String bits2String(byte b, int l) {
        String strBits = "";
        byte mask = 0x01;  //get LSbit

        for (byte bit, i = 1; i <= l; i++) {
            bit = (byte) (b & mask);
            strBits = bit + strBits; //add bit to the left, since LSb first
            b >>= 1;
        }
        return strBits;
    }

    public static int LeBits(int bits) throws IOException {
        int resbits = 0;
        int mascara;
        if (bits <= availBits) {
            mascara = (int) Math.pow(2, (double) (bits)) - 1;
            resbits = (rb & mascara);
            rb = rb >> bits;
            availBits -= bits;
        } else {
            do {
                rb = is.readUnsignedByte() << availBits | rb;
                availBits += 8;
            } while (availBits < bits);

            mascara = (int) Math.pow(2, (double) (bits)) - 1;
            resbits = (rb & mascara);
            rb = rb >> bits;
            availBits -= bits;
        }

        return resbits;
    }

    public static int[] armazenaArray(int HCLEN) throws IOException {
        int t = HCLEN + 4;
        int[] array = new int[19];
        int[] array2 = {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
        for (int i = 0; i < t; i++) {
            array[array2[i]] = LeBits(3);
        }
        return array;
    }

    public static int[] converteHuffman(int[] array) {
        int max = maxArray(array);
        int code = 0;
        int[] bl_count = new int[max + 1];
        int[] array2 = new int[array.length];
        for (int i = 0; i < max + 1; i++) {
            bl_count[i] = 0;
        }
        for (int i = 0; i < array.length; i++) {
            array2[i] = 0;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                bl_count[array[i]]++;
            }
        }
        int[] next_code = new int[max + 1];

        for (int bits = 1; bits <= max; bits++) {
            code = (code + bl_count[bits - 1]) << 1;
            next_code[bits] = code;
        }
        for (int n = 0; n < array.length; n++) {
            int len = array[n];
            if (len != 0) {
                array2[n] = next_code[len];
                next_code[len]++;
            }
        }
        return array2;
    }

    public static int maxArray(int[] array) {
        int max = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static HuffmanTree criaArvore(int[] array, int[] array2) {
        HuffmanTree arvore = new HuffmanTree();
        String s;
        for (int i = 0; i < array.length; i++) {
            s = bits2String((byte) array[i], array2[i]);
            if (!s.equals("")) {
                arvore.addNode(s, i, false);
            }
        }
        return arvore;
    }

    public static void LZ77(int array[], int array2[], int array3[], int array4[]) throws IOException {
        File file = new File(gzh.fName);
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        HuffmanTree arvore = criaArvore(array, array2);
        HuffmanTree arvore2 = criaArvore(array3, array4);
        ArrayList<Integer> ret = new ArrayList<>();
        int extraBits[] = {11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227};
        int extraDistBits[] = {4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144, 8192, 12288, 16384, 24576};
        int pos = 0;
        int aux = 0;
        int auxLeng = 0;
        int posLeng = 0;
        int auxDist = 0;
        int extra = 0;
        boolean end = false;
        boolean sair = false;
        boolean sair2 = false;
        char nextBit;
        while (!end) {
            arvore.resetCurNode();
            while (!sair) {
                aux = LeBits(1);
                if (aux == 1) {
                    nextBit = '1';
                } else {
                    nextBit = '0';
                }
                pos = arvore.nextNode(nextBit);
                if (pos != -2) {
                    sair = true;
                }
            }
            sair = false;

            if (pos < 256) {
                ret.add(pos);
            } else if (pos == 256) {
                end = true;
            } else {
                extra = 0;
                if (pos >= 257 && pos < 265) {
                    auxLeng = pos - 254;
                } else if (pos == 285) {
                    auxLeng = 258;
                } else {
                    posLeng = pos - 265;
                    if (pos >= 265 && pos <= 268) {
                        extra = 1;
                    } else if (pos >= 269 && pos <= 272) {
                        extra = 2;
                    } else if (pos >= 273 && pos <= 276) {
                        extra = 3;
                    } else if (pos >= 277 && pos <= 280) {
                        extra = 4;
                    } else if (pos >= 281 && pos <= 284) {
                        extra = 5;
                    }
                    aux = LeBits(extra);
                    auxLeng = extraBits[posLeng] + aux;
                }

                arvore2.resetCurNode();
                while (!sair2) {
                    aux = LeBits(1);
                    if (aux == 1) {
                        nextBit = '1';
                    } else {
                        nextBit = '0';
                    }
                    pos = arvore2.nextNode(nextBit);
                    if (pos != -2) {
                        sair2 = true;
                    }
                }
                sair2 = false;

                if (pos > 3) {
                    auxDist = (int) Math.floor(((pos - 2) / 2));
                    aux = LeBits(auxDist);
                    auxDist = aux + (extraDistBits[pos - 4]);
                } else {
                    auxDist = pos;
                }

                ArrayList<Integer> arr = new ArrayList<>();
                while (auxLeng > 0) {
                    if (auxDist < 0) {
                        arr.add(arr.get(Math.abs((auxDist + 1))));
                        auxDist--;
                    } else {
                        arr.add(ret.get((ret.size() - 1) - (auxDist--)));
                    }
                    auxLeng--;
                }
                ret.addAll(arr);

            }
        }
        for (int i = 0; i < ret.size(); i++) {
            int temp = ret.get(i);
            writer.write((char) temp);
        }
        writer.close();
    }

    public static int[] literaisComprimentos(int[] array, int[] array2, int end) throws IOException {
        HuffmanTree arvore = criaArvore(array, array2);
        ArrayList<Integer> ret = new ArrayList<>();
        int[] arrayAux = new int[end];
        int pos = 0;
        int ultimo = 0;
        int aux = 0;
        int it = 0;
        boolean sair = false;
        char nextBit;
        int cont = 0;
        while (cont < end) {
            while (!sair) {
                aux = LeBits(1);
                if (aux == 1) {
                    nextBit = '1';
                } else {
                    nextBit = '0';
                }
                pos = arvore.nextNode(nextBit);
                if (pos != -2) {
                    sair = true;
                }
            }
            if (pos < 0) {
                ret.add(0);
                arrayAux[cont] = 0;
                cont++;
            } else {
                if (pos == 16) {
                    aux = LeBits(2);
                    it = aux + 3;
                    for (int i = 0; i < it; i++) {
                        ret.add(ultimo);
                        arrayAux[cont] = ultimo;
                        cont++;
                    }
                } else if (pos == 17) {
                    aux = LeBits(3);
                    it = aux + 3;
                    for (int i = 0; i < it; i++) {
                        ret.add(0);
                        arrayAux[cont] = 0;
                        cont++;
                    }
                } else if (pos == 18) {
                    aux = LeBits(7);
                    it = aux + 11;
                    for (int i = 0; i < it; i++) {
                        ret.add(0);
                        arrayAux[cont] = 0;
                        cont++;
                    }
                } else {
                    ret.add(pos);
                    arrayAux[cont] = pos;
                    cont++;
                    ultimo = pos;
                }
            }
            arvore.resetCurNode();
            sair = false;
        }
        return arrayAux;
    }
}
