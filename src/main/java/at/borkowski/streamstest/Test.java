package at.borkowski.streamstest;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Test {

   static PipedInputStream pis;
   static PipedOutputStream pos;

   static long t0;

   public static void main(String[] args) throws IOException {
      pis = new PipedInputStream(5);
      pos = new PipedOutputStream(pis);
      t0 = System.currentTimeMillis();

      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               threadA();
            } catch (IOException | InterruptedException e) {
               e.printStackTrace();
            }
         }
      }).start();
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               threadB();
            } catch (IOException | InterruptedException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   private static void logA(String message) throws InterruptedException {
      synchronized (System.class) {
         System.out.printf("%6d A: %s\n", System.currentTimeMillis() - t0, message);
         Thread.sleep(1);
      }
   }

   private static void logB(String message) throws InterruptedException {
      synchronized (System.class) {
         System.err.printf("%6d B: %s\n", System.currentTimeMillis() - t0, message);
         Thread.sleep(1);
      }
   }

   private static void threadA() throws IOException, InterruptedException {
      logA("Filling pipe...");
      pos.write(new byte[5]);
      logA("Pipe full. Writing one more byte...");
      pos.write(0);
      logA("Done.");
   }

   private static void threadB() throws IOException, InterruptedException {
      logB("Sleeping a bit...");
      Thread.sleep(100);
      logB("Making space in pipe...");
      pis.read();
      logB("Done.");
   }
}
