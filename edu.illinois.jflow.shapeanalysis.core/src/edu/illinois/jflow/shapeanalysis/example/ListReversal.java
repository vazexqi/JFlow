package edu.illinois.jflow.shapeanalysis.example;

public class ListReversal {
	public static void main(String[] args) {
		Node x= new Node(1, new Node(2, new Node(3, new Node(4, new Node(5, null)))));
		Node reverseX, temp;

		reverseX= null;

		while (x != null) {
			temp= reverseX;
			reverseX= x;
			x= x.cdr;
			reverseX.cdr= temp;
		}
		
		System.out.println(reverseX);
	}
}

class Node {
	int value;

	Node cdr;

	Node(int value, Node cdr) {
		this.value= value;
		this.cdr= cdr;
	}
}
