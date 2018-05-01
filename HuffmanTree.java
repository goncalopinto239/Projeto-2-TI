/* Author: Rui Pedro Paiva
Teoria da Informação, LEI, 2006/2007*/

package gzip;

//class para criação, gestão e acesso de árvores de Huffman
public class HuffmanTree
{
	HFNode root, curNode;  //raíz da árvore e nó actual na travessia
		

	//construtor	
	public HuffmanTree ()
	{
		root = new HFNode(-1, 0, null, null);
		curNode = root;
	}
	
	public HuffmanTree (HFNode rt, HFNode cr)		
	{
		root = rt;
		curNode = cr;		
	}
	

	//reposicionar nó corrente na raíz
	public void resetCurNode ()
	{
		curNode = root;
	}
	
	
	//adiciona nó à árvore 
	//recebe string com o código (representado por String com 0s e 1s) e o índice no alfabeto
	//devolve -1 se o nó já existe; -2 se o código deixar de ser de prefixo; 0 se adicionou bem
	public int addNode(String s, int ind, boolean verbose)
	{
		HFNode tmp = root;
		int lv = 0, len = s.length(), index;
		boolean found = false;
		char direction;
		int pos = -3;
					
		while(lv < len && !found)
		{			
			if (tmp.index != -1)  //tentando criar filho de folha --> deixaria de ser código de prefixo...
			{
				pos = -2;
				found = true;
			}
			else
			{			
				direction = s.charAt(lv);
				
				if (direction == '0')
				{
					if (lv != len-1 && tmp.left != null)  //continua descida
					{					
						tmp = tmp.left;
					}
					else if (tmp.left != null) // nó já inserido
					{
						pos = -1;
						found = true;
					}
					/*else if (tmp.index != -1)  //tentando criar filho de folha --> deixaria de ser código de prefixo...
					{
						pos = -2;
						found = true;
					}*/
					else //cria nó à esquerda
					{
						if (lv == len-1)  //folha						
							index = ind;
						else
							index = -1;

						HFNode hf = new HFNode(index, lv+1, null, null);
						tmp.left = hf;
						tmp = tmp.left;
					}
				}
				else if (direction == '1')
				{				
					if (lv != len -1 && tmp.right != null)
					{					
						tmp = tmp.right;
					}
					else if (tmp.right != null) // nó já inserido
					{
						  pos = -1;
						  found = true;
					}
					/*else if (tmp.index != -1)
					{
						pos = -2;
						found = true;	
					}*/
					else //cria nó à direita
					{
						if (lv == len-1)  //folha
							index = ind;
						else
							index = -1;

						HFNode hf = new HFNode(index, lv+1, null, null);
						tmp.right = hf;
						tmp = tmp.right;
					}			
				}
			}				
			lv++;				
		}
		
				
		if (!found)
			pos = tmp.index;
			
		if (verbose)
		{
			if (pos == -1)
				System.out.println("Código '" + s + "' já inserido!!!");
			else if (pos == -2)
				System.out.println("Código '" + s + ": tentando extender folha!!!");
			else
				System.out.println("Código '" + s + "' inserido com sucesso");
		}
		
		return pos;
	}				
		
	
	//procura código na árvore, a partir do nó actual (representado por String com 0s e 1s
	//devolve -1 se não encontrou; -2 se é prefixo de código existente; índice no alfabeto se encontrou
	public int findNode(String s, HFNode cur, boolean verbose)
	{
		HFNode tmp = cur;
		int lv = 0, len = s.length();
		boolean found = true;
		int pos;
		
		while(lv < len && found)
		{			
			char direction = s.charAt(lv);
			
			if (direction == '0')
			{
				if (tmp.left != null)
					tmp = tmp.left;
				else
					found = false;
			}
			else if (direction == '1')
			{
				if (tmp.right != null)
					tmp = tmp.right;
				else
					found = false;
			}
			
			lv++;
		}
				
		if (!found)
			pos = -1;
		else if(tmp.index == -1)
			pos = -2;
		else
			pos = tmp.index;
			
		if (verbose)
		{
			if (pos == -1)
				System.out.println("Código '" + s + "' não encontrado!!!");
			else if (pos == -2)
				System.out.println("Código '" + s + "': não encontrado mas prefixo!!!");
			else
				System.out.println("Código '" + s + "' corresponde à posição " + pos + " do alfabeto");
		}
						
		return pos;
	}	
	
	//procura código na árvore a partir da raíz (representado por String com 0s e 1s
	public int findNode(String s, boolean verbose)
	{
		return findNode(s, root, verbose);
	}
	
	//actualiza nó corrente na árvore com base no nó actual e nó próximo bit
	//devolve -1 se não encontrou o nó, -2 se encontrou mas não é folha, index se é folha
	public int nextNode(char c)
	{
		int pos;
		
		if (curNode.isLeaf())
			pos = -1;
		
		if (c == '0')
			if (curNode.left != null)
			{
				curNode = curNode.left;
				if (curNode.isLeaf())
					pos = curNode.index;
				else
					pos = -2;
			}
			else
				pos = -1;
		else // c == 1
			if (curNode.right != null)
			{
				curNode = curNode.right;
				if (curNode.isLeaf())
					pos = curNode.index;
				else
					pos = -2;
			}
			else
				pos = -1;								
		
		return pos;
	}
}


/*------------------------------------------------------------------------------*/

class HFNode
{
	int index = -1;  //se folha, guarda posição no alfabeto; senão, -1;
	int level = 0; // nível do nó na árvore
	HFNode left, right;  //referências para os filhos direito e esquerdo: é folha se ambos forem null	
	
	HFNode (int i, int lv)
	{
		this(i, lv, null, null);
	}
	
	HFNode (int i, int lv, HFNode l, HFNode r)
	{
		index = i;
		level = lv;
		left = l;
		right = r;
	}
	
	
	//verifica se o nó é folha
	boolean isLeaf()
	{
		if (left == null && right == null)
			return true;
		else
			return false;
	}	
}