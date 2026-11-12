
import javax.swing.JFrame;
public class Main {
    public static void main(String[] args) {
        VentanaPasajes ventana = new VentanaPasajes();
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(850, 590);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
}
