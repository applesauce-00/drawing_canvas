import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Stack;

public class DrawingCanvas {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Interactive Drawing Canvas");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // Create the drawing panel
            DrawingPanel drawingPanel = new DrawingPanel();
            frame.add(drawingPanel, BorderLayout.CENTER);

            // Create buttons for the bottom panel
            JButton undoButton = new JButton("Undo");
            undoButton.addActionListener(e -> drawingPanel.undo());

            JButton redoButton = new JButton("Redo");
            redoButton.addActionListener(e -> drawingPanel.redo());

            JButton clearButton = new JButton("Clear Canvas");
            clearButton.addActionListener(e -> drawingPanel.clearCanvas());

            JButton colorButton = new JButton("Select Color");
            colorButton.addActionListener(e -> {
                Color selectedColor = JColorChooser.showDialog(frame, "Choose a Color", drawingPanel.getPenColor());
                if (selectedColor != null) {
                    drawingPanel.setPenColor(selectedColor);
                }
            });

            JComboBox<String> shapeSelector = new JComboBox<>(new String[]{"Free Draw", "Rectangle", "Oval"});
            shapeSelector.addActionListener(e -> drawingPanel.setShapeMode((String) shapeSelector.getSelectedItem()));

            JSlider thicknessSlider = new JSlider(1, 10, 2);
            thicknessSlider.setMajorTickSpacing(1);
            thicknessSlider.setPaintTicks(true);
            thicknessSlider.setPaintLabels(true);
            thicknessSlider.addChangeListener(e -> drawingPanel.setBrushThickness(thicknessSlider.getValue()));

            JButton saveButton = new JButton("Save Drawing");
            saveButton.addActionListener(e -> {
                try {
                    drawingPanel.saveCanvas();
                    JOptionPane.showMessageDialog(frame, "Drawing saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to save the drawing: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Add buttons to the panel
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(undoButton); // Add Undo button
            buttonPanel.add(redoButton); // Add Redo button
            buttonPanel.add(clearButton);
            buttonPanel.add(colorButton);
            buttonPanel.add(new JLabel("Shape Tool:"));
            buttonPanel.add(shapeSelector);
            buttonPanel.add(new JLabel("Brush Thickness:"));
            buttonPanel.add(thicknessSlider);
            buttonPanel.add(saveButton);

            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }
}

class DrawingPanel extends JPanel {
    private final ArrayList<ColoredShape> shapes = new ArrayList<>();
    private Point startPoint = null;
    private Point currentPoint = null;
    private Color penColor = Color.BLACK;
    private String shapeMode = "Free Draw";
    private int brushThickness = 2;
    private boolean isFreeDrawing = false;
    private final Stack<ArrayList<ColoredShape>> undoStack = new Stack<>();
    private final Stack<ArrayList<ColoredShape>> redoStack = new Stack<>();

    public DrawingPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));

        // Add mouse listeners for drawing
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                currentPoint = startPoint;
                undoStack.push(new ArrayList<>(shapes));
                redoStack.clear(); // Clear redo stack whenever a new stroke is made
                if ("Free Draw".equals(shapeMode)) {
                    isFreeDrawing = true;
                    shapes.add(new ColoredShape(startPoint, startPoint, penColor, "Line", brushThickness));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (startPoint != null && !"Free Draw".equals(shapeMode)) {
                    Point endPoint = e.getPoint();
                    if ("Rectangle".equals(shapeMode)) {
                        shapes.add(new ColoredShape(startPoint, endPoint, penColor, "Rectangle", brushThickness));
                    } else if ("Oval".equals(shapeMode)) {
                        shapes.add(new ColoredShape(startPoint, endPoint, penColor, "Oval", brushThickness));
                    }
                }
                startPoint = null;
                currentPoint = null;
                isFreeDrawing = false;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if ("Free Draw".equals(shapeMode) && isFreeDrawing) {
                    Point endPoint = e.getPoint();
                    shapes.add(new ColoredShape(startPoint, endPoint, penColor, "Line", brushThickness));
                    startPoint = endPoint;
                } else if (startPoint != null) {
                    currentPoint = e.getPoint();
                }
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (ColoredShape shape : shapes) {
            g2d.setColor(shape.getColor());
            g2d.setStroke(new BasicStroke(shape.getThickness()));
            switch (shape.getType()) {
                case "Rectangle":
                    g2d.drawRect(Math.min(shape.getStart().x, shape.getEnd().x),
                                 Math.min(shape.getStart().y, shape.getEnd().y),
                                 Math.abs(shape.getStart().x - shape.getEnd().x),
                                 Math.abs(shape.getStart().y - shape.getEnd().y));
                    break;
                case "Oval":
                    g2d.drawOval(Math.min(shape.getStart().x, shape.getEnd().x),
                                 Math.min(shape.getStart().y, shape.getEnd().y),
                                 Math.abs(shape.getStart().x - shape.getEnd().x),
                                 Math.abs(shape.getStart().y - shape.getEnd().y));
                    break;
                case "Line":
                    g2d.drawLine(shape.getStart().x, shape.getStart().y, shape.getEnd().x, shape.getEnd().y);
                    break;
            }
        }

        if (startPoint != null && currentPoint != null && !"Free Draw".equals(shapeMode)) {
            g2d.setColor(penColor);
            g2d.setStroke(new BasicStroke(brushThickness));
            switch (shapeMode) {
                case "Rectangle":
                    g2d.drawRect(Math.min(startPoint.x, currentPoint.x),
                                 Math.min(startPoint.y, currentPoint.y),
                                 Math.abs(startPoint.x - currentPoint.x),
                                 Math.abs(startPoint.y - currentPoint.y));
                    break;
                case "Oval":
                    g2d.drawOval(Math.min(startPoint.x, currentPoint.x),
                                 Math.min(startPoint.y, currentPoint.y),
                                 Math.abs(startPoint.x - currentPoint.x),
                                 Math.abs(startPoint.y - currentPoint.y));
                    break;
            }
        }
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new ArrayList<>(shapes));
            shapes.clear();
            shapes.addAll(undoStack.pop());
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new ArrayList<>(shapes));
            shapes.clear();
            shapes.addAll(redoStack.pop());
            repaint();
        }
    }

    public void clearCanvas() {
        shapes.clear();
        repaint();
    }

    public void setPenColor(Color color) {
        this.penColor = color;
    }

    public Color getPenColor() {
        return penColor;
    }

    public void setShapeMode(String mode) {
        this.shapeMode = mode;
    }

    public void setBrushThickness(int thickness) {
        this.brushThickness = thickness;
    }

    public void saveCanvas() throws IOException {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        paint(g2d);
        g2d.dispose();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Drawing");
        fileChooser.setSelectedFile(new File("drawing.png"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }
            ImageIO.write(image, "png", fileToSave);
            JOptionPane.showMessageDialog(this, "Drawing saved to: " + fileToSave.getAbsolutePath(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

class ColoredShape {
    private final Point start;
    private final Point end;
    private final Color color;
    private final String type;
    private final int thickness;

    public ColoredShape(Point start, Point end, Color color, String type, int thickness) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.type = type;
        this.thickness = thickness;
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public Color getColor() {
        return color;
    }

    public String getType() {
        return type;
    }

    public int getThickness() {
        return thickness;
    }
}
