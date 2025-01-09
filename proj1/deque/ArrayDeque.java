package deque;

public class ArrayDeque<T> {
    private T[] deque;
    private int size;
    public ArrayDeque(){
        deque = (T[]) new Object[8];
        size = 0;
    }
    public void addFirst(T item){
        if(size == deque.length)resize(size*2);
        for(int i = 0;i<size;i++){
            deque[i+1] =deque[i];
        }
        deque[0] = item;
        size +=1;
    }
    public void addLast(T item){
        if(size == deque.length)resize(size*2);
        deque[size++]=item;
    }
    public boolean isEmpty(){return size==0;}
    public int size(){return size;}
    public void printDeque(){
        for(int i =0;i<size;i++){
            System.out.print(deque[i]+" ");
        }
        System.out.println();
    }
    public T removeFirst(){
        if(size == 0)return null;
        T item = deque[0];
        for(int i =1;i<size;i++){
            deque[i-1]=deque[i];
        }
        size--;
        if(size<deque.length/4)resize(deque.length/2);
        return item;
    }
    public T removeLast(){
        if(size == 0)return null;
        T item = deque[size-1];
        size--;
        if(size<deque.length/4)resize(deque.length/2);
        return item;
    }
    public T get(int index){
        if(index<0 || index>=size)return null;
        return deque[index];
    }
    public void resize(int newSize){
        T[] newDeque = (T[])new Object[newSize];
        for(int i = 0; i < size; i++){
            newDeque[i] = deque[i];
        }
        deque = newDeque;
    }
}
