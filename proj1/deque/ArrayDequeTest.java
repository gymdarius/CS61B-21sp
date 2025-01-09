package deque;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayDequeTest {

    @Test
    public void fillEmptyFill(){
        ArrayDeque d = new ArrayDeque();
        for(int i=0;i<8;i++)d.addFirst(i);
        assertEquals(8,d.size());
        for(int i=0;i<8;i++)d.removeFirst();
        assertEquals(0,d.size());
        for(int i=0;i<8;i++)d.addLast(i);
        assertEquals(8,d.size());
    }
}
