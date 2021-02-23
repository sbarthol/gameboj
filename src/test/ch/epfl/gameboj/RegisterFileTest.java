
package ch.epfl.gameboj;
import org.junit.jupiter.api.Test;

public class RegisterFileTest {
    
    enum Regss implements Register {
        a,b,c
    }
    
    @Test
    void testSomething() {
       
        for(int i=0;i<Regss.values().length;i++) {
            System.out.println(Regss.values()[i]);
            System.out.println(Regss.values()[i].getClass().toString());
        }
    }

}
