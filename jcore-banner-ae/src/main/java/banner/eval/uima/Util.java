package banner.eval.uima;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Util {
  public InputStream getStream(String path){
    URL url = getClass().getClassLoader().getResource(path);
    try {
      return url.openStream();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
  
   public String getFile(String path){
      URL url = getClass().getClassLoader().getResource(path);
      System.out.println("URL:" + url.getFile()); 
      return url.getFile();
    }
}
