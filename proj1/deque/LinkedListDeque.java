package deque;

public class LinkedListDeque<T>implements Deque<T> {

    private class Node{
        Node prev;
        T item;
        Node next;
        public Node(T item, Node prev, Node next) {
            this.item = item;
            this.prev = prev;
            this.next = next;
        }
    }
    private int size=0;
    private Node head;

    public LinkedListDeque(){
        head = new Node(null,null,null);
        head.next = head;
        head.prev = head;
        size=0;
    }
    public void addFirst(T item){
        Node newNode = new Node(item,head,head.next);
        head.next = newNode;
        head.next.next.prev = head.next;
        size++;
    }
    public void addLast(T item){
        Node newNode = new Node(item,head.prev,head);
        head.prev.next = newNode;
        head.prev = newNode;
        size++;
    }

    public int size(){
        return size;
    }
    public void printDeque(){
        Node current = head.next;
        while(current!=head){
            System.out.print(current.item+" ");
            current = current.next;
        }
    }
    public T removeFirst(){
        if(isEmpty()){return null;}
        T item = head.next.item;
        head.next.next.prev = head;
        head.next = head.next.next;
        size--;
        return item;
    }
    public T removeLast(){
        if(isEmpty()){return null;}
        T item = head.prev.item;
        head.prev.prev.next = head;
        head.prev = head.prev.prev;
        size--;
        return item;
    }
    public T get(int index){
        if(index>=size){return null;}
        Node current = head.next;
        for(int i=0; i<index; i++){
            current = current.next;
        }
        return current.item;
    }
    public T getRecursive(int index){
        if (index>=size)
            return null;
        return getRecursive(0,index,head.next);
    }
    private T getRecursive(int pos,int index,Node x){
        if (pos==index)
            return x.item;
        return getRecursive(pos+1,index,x.next);
    }
}
