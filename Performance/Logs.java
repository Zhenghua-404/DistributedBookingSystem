import java.io.*;

public class Logs {
    public PrintWriter pw;

    public Logs(String filename){
		try {
			File file =new File(filename);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			this.pw = new PrintWriter(bw);	 
		}catch (IOException e){
			e.printStackTrace();
			System.exit(1);
		}
    }

    public void write(String out){
        pw.println(out);
    }

    public void close(){
        pw.close();
    }
    

    
}
