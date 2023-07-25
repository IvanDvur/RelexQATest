package utils;

import java.util.Random;

public class RandomStringGenerator {


   public static String generateRandomUsername(int length){
       int leftLimit = 97; // буква 'a'
       int rightLimit = 122; // буква 'z'
       Random random = new Random();

       String generatedString = random.ints(leftLimit, rightLimit + 1)
               .limit(length)
               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
               .toString();

       return generatedString;
   }
}
