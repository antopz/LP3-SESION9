
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class VentanaPasajes extends JFrame {
    private JTextField txtNombre;
    private JTextField txtDocumento;
    private JTextField txtFechaIda;
    private JTextField txtFechaRetorno;
    private JComboBox<String> cmbOrigen;
    private JComboBox<String> cmbDestino;
    private JCheckBox chkAudifonos;
    private JCheckBox chkManta;
    private JCheckBox chkRevistas;
    private JRadioButton rbPrimerPiso;
    private JRadioButton rbSegundoPiso;
    private ButtonGroup grupoPisos;
    private JList<String> listaServicio;
    private JButton btnResumen;
    private JButton btnLimpiar;

    public VentanaPasajes() {
        super("Compra de pasajes - Transporte terrestre");
        setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("REGISTRO DE PASAJES", JLabel.CENTER);
        add(titulo, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(9, 2, 5, 5));

        txtNombre = new JTextField(20);
        txtDocumento = new JTextField(20);
        txtFechaIda = new JTextField(20);
        txtFechaRetorno = new JTextField(20);

        String[] ciudades = {"Seleccione", "Arequipa", "Lima", "Cusco", "Puno", "Tacna"};
        cmbOrigen = new JComboBox<String>(ciudades);
        cmbDestino = new JComboBox<String>(ciudades);

        formulario.add(new JLabel("Nombres y apellidos:"));
        formulario.add(txtNombre);

        formulario.add(new JLabel("Documento de identidad:"));
        formulario.add(txtDocumento);

        formulario.add(new JLabel("Fecha de ida (aaaa-mm-dd):"));
        formulario.add(txtFechaIda);

        formulario.add(new JLabel("Fecha de retorno (aaaa-mm-dd):"));
        formulario.add(txtFechaRetorno);

        formulario.add(new JLabel("Ciudad de origen:"));
        formulario.add(cmbOrigen);

        formulario.add(new JLabel("Ciudad de destino:"));
        formulario.add(cmbDestino);

        rbPrimerPiso = new JRadioButton("Primer piso", true);
        rbSegundoPiso = new JRadioButton("Segundo piso");

        grupoPisos = new ButtonGroup();
        grupoPisos.add(rbPrimerPiso);
        grupoPisos.add(rbSegundoPiso);

        JPanel panelPiso = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelPiso.add(rbPrimerPiso);
        panelPiso.add(rbSegundoPiso);

        formulario.add(new JLabel("Ubicacion del asiento:"));
        formulario.add(panelPiso);

        chkAudifonos = new JCheckBox("Audifonos");
        chkManta = new JCheckBox("Manta");
        chkRevistas = new JCheckBox("Revistas");

        JPanel panelExtras = new JPanel(new GridLayout(1, 3));        
        panelExtras.add(chkAudifonos);
        panelExtras.add(chkManta);
        panelExtras.add(chkRevistas);

        formulario.add(new JLabel("Servicios opcionales:"));
        formulario.add(panelExtras);

        String[] servicios = {"Economico", "Standard", "VIP"};
        listaServicio = new JList<String>(servicios);
        listaServicio.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaServicio.setSelectedIndex(0);

        formulario.add(new JLabel("Calidad de servicio:"));
        formulario.add(new JScrollPane(listaServicio));

        add(formulario, BorderLayout.CENTER);

        btnResumen = new JButton("Mostrar resumen");
        btnLimpiar = new JButton("Limpiar");

        JPanel panelBotones = new JPanel(new FlowLayout());
        panelBotones.add(btnResumen);
        panelBotones.add(btnLimpiar);

        add(panelBotones, BorderLayout.SOUTH);

        ManejadorBotones manejador = new ManejadorBotones();
        btnResumen.addActionListener(manejador);
        btnLimpiar.addActionListener(manejador);
    }

    private void mostrarResumen() {
        String nombre = txtNombre.getText().trim();
        String documento = txtDocumento.getText().trim();
        String fechaIda = txtFechaIda.getText().trim();
        String fechaRetorno = txtFechaRetorno.getText().trim();
        String origen = (String) cmbOrigen.getSelectedItem();
        String destino = (String) cmbDestino.getSelectedItem();

        if (nombre.isEmpty() || documento.isEmpty()
                || fechaIda.isEmpty() || fechaRetorno.isEmpty()
                || cmbOrigen.getSelectedIndex() == 0
                || cmbDestino.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Complete todos los datos del pasajero.");
            return;
        }

        // VALIDACION DE FECHAS
        try {
            LocalDate ida = LocalDate.parse(fechaIda);
            LocalDate retorno = LocalDate.parse(fechaRetorno);

            if (ida.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "La fecha de ida no puede ser anterior a hoy.");
                return;
            }

            if (retorno.isBefore(ida)) {
                JOptionPane.showMessageDialog(this, "La fecha de retorno no puede ser anterior a la ida.");
                return;
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fecha incorrecta. Use aaaa-mm-dd.");
            return;
        }

        if (origen.equals(destino)) {
            JOptionPane.showMessageDialog(this, "El origen y el destino deben ser diferentes.");
            return;
        }

        String piso = "";

        if (rbPrimerPiso.isSelected()) {
            piso = "Primer piso";
        } else {
            piso = "Segundo piso";
        }

        String extras = "";

        if (chkAudifonos.isSelected()) {
            extras += "Audifonos, ";
        }

        if (chkManta.isSelected()) {
            extras += "Manta, ";
        }

        if (chkRevistas.isSelected()) {
            extras += "Revistas, ";
        }

        if (extras.isEmpty()) {
            extras = "Ninguno";
        } else {
            extras = extras.substring(0, extras.length() - 2);
        }

        String resumen = "RESUMEN DE COMPRA\n\n"
                + "Pasajero: " + nombre + "\n"
                + "Documento: " + documento + "\n"
                + "Fecha de ida: " + fechaIda + "\n"
                + "Fecha de retorno: " + fechaRetorno + "\n"
                + "Origen: " + origen + "\n"
                + "Destino: " + destino + "\n"
                + "Ubicacion: " + piso + "\n"
                + "Servicio: " + listaServicio.getSelectedValue() + "\n"
                + "Opcionales: " + extras;

        JOptionPane.showMessageDialog(this, resumen, "Datos del pasaje",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void limpiar() {
        txtNombre.setText("");
        txtDocumento.setText("");
        txtFechaIda.setText("");
        txtFechaRetorno.setText("");
        cmbOrigen.setSelectedIndex(0);
        cmbDestino.setSelectedIndex(0);
        rbPrimerPiso.setSelected(true);
        chkAudifonos.setSelected(false);
        chkManta.setSelected(false);
        chkRevistas.setSelected(false);
        listaServicio.setSelectedIndex(0);
        txtNombre.requestFocus();
    }

    private class ManejadorBotones implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evento) {
            if (evento.getSource() == btnResumen) {
                mostrarResumen();
            } else if (evento.getSource() == btnLimpiar) {
                limpiar();
            }
        }
    }
}
