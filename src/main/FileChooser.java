package main;
import lwjglutils.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class FileChooser {
    //https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
    public static OGLTexture2D loadTexture(){
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("./res/textures"));
        chooser.setFileFilter(new FileNameExtensionFilter("Texture", "jpg","png"));
        chooser.setAcceptAllFileFilterUsed(false);
        int result = chooser.showOpenDialog(null);
        if(result== JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath().replace("\\", "/");
            String[] name = path.split("/");
            try {
                return  new OGLTexture2D("textures/"+name[name.length-1]);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        else if(result == JFileChooser.CANCEL_OPTION)
        {System.out.println("Wrong file bruh");}
        return  null;
    }
}
