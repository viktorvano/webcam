package webcam;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Label;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Main extends Application implements WebcamListener {
    private Webcam webcam;
    private BufferedImage bufferedImage;
    private Image image;
    private ImageView imageView;
    private boolean showMotionEffect = false, addRGB = false, monochromatic = false;
    private float motion = 0f;
    private float threshold = 2.0f;
    private boolean thresholdFlag = false;
    private int motionCount = 0;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.addWebcamListener(Main.this);
        webcam.open();

        bufferedImage = webcam.getImage();

        image = SwingFXUtils.toFXImage(bufferedImage, null);

        imageView = new ImageView();
        imageView.setImage(image);
        imageView.setLayoutX(6);
        imageView.setLayoutY(6);

        Label labelMotion = new Label();
        labelMotion.setLayoutX(10);
        labelMotion.setLayoutY(500);
        labelMotion.setFont(Font.font("Arial", 24));

        CheckBox checkBoxMonochromatic = new CheckBox("Monochromatic Mode");
        CheckBox checkBoxMotion = new CheckBox("Motion Mode");
        CheckBox checkBoxAddRGB = new CheckBox("Show Colors");

        checkBoxMonochromatic.setLayoutX(10);
        checkBoxMonochromatic.setLayoutY(540);
        checkBoxMonochromatic.setOnAction(event -> {
            monochromatic = checkBoxMonochromatic.isSelected();
            checkBoxMotion.setDisable(monochromatic);
        });

        checkBoxMotion.setLayoutX(10);
        checkBoxMotion.setLayoutY(580);
        checkBoxMotion.setOnAction(event -> {
            showMotionEffect = checkBoxMotion.isSelected();
            if(showMotionEffect){
                checkBoxMonochromatic.setDisable(true);
                checkBoxAddRGB.setDisable(false);
            }else {
                checkBoxMonochromatic.setDisable(false);
                checkBoxAddRGB.setDisable(true);
                checkBoxAddRGB.setSelected(false);
                addRGB = false;
            }
        });

        checkBoxAddRGB.setLayoutX(10);
        checkBoxAddRGB.setLayoutY(620);
        checkBoxAddRGB.setOnAction(event -> addRGB = checkBoxAddRGB.isSelected());
        checkBoxAddRGB.setDisable(true);

        Label labelThreshold = new Label(threshold + " %");
        labelThreshold.setLayoutX(240);
        labelThreshold.setLayoutY(590);
        labelThreshold.setFont(Font.font("Arial", 20));

        Label labelLiterallyJustThreshold = new Label("Threshold");
        labelLiterallyJustThreshold.setLayoutX(240);
        labelLiterallyJustThreshold.setLayoutY(540);
        labelLiterallyJustThreshold.setFont(Font.font("Arial", 20));

        Slider sliderThreshold =  new Slider();
        sliderThreshold.setMax(3.0);
        sliderThreshold.setValue(threshold);
        sliderThreshold.setLayoutX(220);
        sliderThreshold.setLayoutY(570);
        sliderThreshold.valueProperty().addListener((obs, oldValue, newValue) -> {
            threshold = newValue.floatValue();
            labelThreshold.setText(threshold + " %");
        });

        Button buttonResetCounter = new Button("Reset Counter");
        buttonResetCounter.setLayoutX(420);
        buttonResetCounter.setLayoutY(570);
        buttonResetCounter.setOnAction(event -> motionCount = 0);

        Label labelMotionCount = new Label("Motion count: " + motionCount);
        labelMotionCount.setLayoutX(420);
        labelMotionCount.setLayoutY(610);
        labelMotionCount.setFont(Font.font("Arial", 20));

        Pane pane = new Pane();
        pane.setPrefSize(640, 700);
        pane.setStyle("-fx-background-color: #7F7F7F");
        pane.getChildren().add(imageView);
        pane.getChildren().add(labelMotion);
        pane.getChildren().add(checkBoxMonochromatic);
        pane.getChildren().add(checkBoxMotion);
        pane.getChildren().add(checkBoxAddRGB);
        pane.getChildren().add(labelThreshold);
        pane.getChildren().add(labelLiterallyJustThreshold);
        pane.getChildren().add(sliderThreshold);
        pane.getChildren().add(buttonResetCounter);
        pane.getChildren().add(labelMotionCount);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Webcam Viewer with effects");
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            if(showMotionEffect){
                labelMotion.setText("Motion: " + motion + "%");
            }else {
                labelMotion.setText("");
            }

            labelMotionCount.setText("Motion count: " + motionCount);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("Closing Application.");
    }

    @Override
    public void webcamOpen(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamClosed(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamDisposed(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamImageObtained(WebcamEvent webcamEvent) {
        if(showMotionEffect)
        {
            float motionCalculation = 0f;
            BufferedImage bufferedImageNew = webcam.getImage();
            BufferedImage bufferedImageMonoChromatic = deepCopy(bufferedImageNew);
            setMonochromatic(bufferedImageMonoChromatic);
            BufferedImage bufferedImageResult = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);//new BufferedImage(bufferedImageNew.getWidth(), bufferedImageNew.getHeight(), bufferedImageNew.getType());
            int color;
            for(int width = 0; width < bufferedImageResult.getWidth(); width++)
                for(int height = 0; height < bufferedImageResult.getHeight(); height++) {
                    color = Math.abs(bufferedImage.getRGB(width, height) - bufferedImageMonoChromatic.getRGB(width, height));
                    bufferedImageResult.setRGB(width, height, color);
                    motionCalculation += (((float)color*100.0f)/16777215.0f);
                }
            motionCalculation /= (bufferedImageResult.getWidth()*bufferedImageResult.getHeight());
            motion = motionCalculation;
            if(!thresholdFlag && motion > threshold)
                thresholdFlag = true;
            else if(thresholdFlag && motion < threshold)
            {
                thresholdFlag = false;
                motionCount++;
            }

            if(addRGB)
            for(int width = 0; width < bufferedImageResult.getWidth(); width++)
                for(int height = 0; height < bufferedImageResult.getHeight(); height++) {
                    color = bufferedImageResult.getRGB(width, height) + bufferedImageNew.getRGB(width, height);
                    bufferedImageResult.setRGB(width, height, color);
                }
            image = SwingFXUtils.toFXImage(bufferedImageResult, null);
            imageView.setImage(image);
            bufferedImage = bufferedImageMonoChromatic;
        }else
        {
            bufferedImage = webcam.getImage();
            if(monochromatic)
                setMonochromatic(bufferedImage);
            image = SwingFXUtils.toFXImage(bufferedImage, null);
            imageView.setImage(image);
        }
    }

    private void setMonochromatic(BufferedImage bufferedImageMono)
    {
        int color;
        Color c;
        int red, green, blue;
        for(int width = 0; width < bufferedImageMono.getWidth(); width++)
            for(int height = 0; height < bufferedImageMono.getHeight(); height++) {
                c = new Color(bufferedImageMono.getRGB(width, height));
                red = c.getRed();
                green = c.getGreen();
                blue = c.getBlue();
                color = (red + green + blue) / 3;
                color = color*65536 + color*256 + color;
                bufferedImageMono.setRGB(width, height, color);
            }
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage bufferedImageReturn = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        int color;
        for(int width = 0; width < bi.getWidth(); width++)
            for(int height = 0; height < bi.getHeight(); height++) {
                color = bi.getRGB(width, height);
                bufferedImageReturn.setRGB(width, height, color);
            }
        return bufferedImageReturn;
    }
}
