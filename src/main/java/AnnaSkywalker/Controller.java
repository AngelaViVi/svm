package AnnaSkywalker;

import AnnaSkywalker.svm.Classification;
import AnnaSkywalker.svm.PointPair;
import AnnaSkywalker.svm.SolutionNotFoundException;
import AnnaSkywalker.svm.SupportVectorMachine;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    //界面元素
    private LineChart<Number, Number> sc;
    private BorderPane pane;
    private StackPane stackPane;
    private Button trainButton;
    private Button buttonTesting;
    private TextField precisionText;
    //点集
    private XYChart.Series testPositiveSeries;
    private XYChart.Series testNegativeSeries;
    //读取输入数据
    private final DataFileFactory dataFileFactory;
    //核心计算
    private SupportVectorMachine svm;
    //存储训练数据和测试数据
    private ListView<String> testingDataList;
    private TestingData testingData;
    private ListView<String> trainingDataList;
    private TrainingData trainingData;


    /*
    构造函数
     */
    public Controller() {
        this.dataFileFactory = new DataFileFactory();
        this.svm = new SupportVectorMachine();
    }

    /*
    设置界面窗口的一些属性
     */
    public void setStage(Stage stage) {
        stage.setTitle("Sample SVM with java");//窗口标题
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        this.sc = new LineChart<Number, Number>(xAxis, yAxis);
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        sc.setTitle("Sample SVM with java");//坐标图上放标题

        this.pane = new BorderPane();;
        pane.setCenter(sc);
        pane.setTop(this.createTopPanel());
        pane.setLeft(this.createLeftPanel());

        this.stackPane = new StackPane();
        this.stackPane.getChildren().add(pane);

        Scene main = new Scene(this.stackPane);
        main.getStylesheets().add("/css/graph.css");
        stage.setScene(main);
        stage.setMaximized(false);//窗口最大化
        stage.setHeight(600);//窗口尺寸
        stage.setWidth(1100);
        stage.show();

        this.stateInit();
    }
    /*
    调整界面元素的可用状态:初始化
     */
    private void stateInit() {
        this.trainButton.setDisable(true);
        this.buttonTesting.setDisable(true);

        this.clearPlot();
        this.testingDataList.getItems().clear();
        this.trainingDataList.getItems().clear();
    }
    /*
    调整界面元素的可用状态:训练数据载入成功后
    */
    private void stateTrainingLoaded() {
        this.trainButton.setDisable(false);
        this.buttonTesting.setDisable(true);
    }
    /*
    调整界面元素的可用状态:训练结束后
     */
    private void stateTrained() {
        this.trainButton.setDisable(true);
        this.buttonTesting.setDisable(false);
    }
    /*
    创建左侧的容器,向其中添加两个listview来显示测试数据和训练数据
     */
    private HBox createLeftPanel() {
        HBox hbox = new HBox();

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Text title = new Text("训练数据");
        this.trainingDataList = new ListView<String>();
        this.trainingDataList.setPrefHeight(910);
        vbox.getChildren().addAll(title, this.trainingDataList);

        VBox vbox2 = new VBox();
        vbox2.setPadding(new Insets(10));
        vbox2.setSpacing(8);

        Text title2 = new Text("测试数据");
        this.testingDataList = new ListView<String>();
        this.testingDataList.setPrefHeight(910);
        vbox2.getChildren().addAll(title2, this.testingDataList);

        hbox.getChildren().addAll(vbox, vbox2);


        return hbox;
    }
    /*
    创建上部的容器,向其中添加按钮等部件
    */
    private HBox createTopPanel() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);

        Button buttonCurrent = new Button("载入训练数据");
        buttonCurrent.setPrefSize(150, 20);
        buttonCurrent.setOnAction((e) -> {
            FileChooser fileChooser = new FileChooser();
            File initDirectory=new File(".\\src\\main\\Data");//提供文件选择对话框的初始位置
            fileChooser.setInitialDirectory(initDirectory);
            File file = fileChooser.showOpenDialog(null);

            if (file != null) {
                try {
                    this.stateInit();
                    this.loadTrainingFile(file);
                } catch (IOException | UnknownDataFileException e1) {
                    this.showError(e1.getMessage());
                }
            }
        });

        this.trainButton = new Button("训练");
        this.trainButton.setOnAction((e) -> {
            try {
                this.clearPlot();
                this.plotTrainingData(this.trainingData);
                this.train(this.trainingData, Integer.parseInt(precisionText.getText()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.trainButton.setPrefSize(100, 20);
        this.precisionText = new TextField("4");
        precisionText.setPrefColumnCount(2);
        precisionText.textProperty().addListener(e -> {
            if(this.trainingDataList.getItems().size() > 0) {
                this.stateTrainingLoaded();
            }
        });
        Text text = new Text("精度: ");
        text.setTranslateY(5);
        text.setTranslateX(10);

        this.buttonTesting = new Button("载入测试数据");
        buttonTesting.setPrefSize(150, 20);
        buttonTesting.setOnAction((e) -> {
            FileChooser fileChooser = new FileChooser();
            File initDirectory=new File(".\\src\\main\\Data");
            fileChooser.setInitialDirectory(initDirectory);
            File file = fileChooser.showOpenDialog(null);

            if (file != null) {
                try {
                    this.loadTestingFile(file);
                } catch (IOException | UnknownDataFileException e1) {
                    this.showError(e1.getMessage());
                }
            }
        });

        hbox.getChildren().addAll(buttonCurrent, text, precisionText, this.trainButton, buttonTesting);

        return hbox;
    }

    private void clearPlot() {
        this.sc.setAnimated(false);
        this.testPositiveSeries = new XYChart.Series();
        this.testPositiveSeries.setName("Test Positive");

        this.testNegativeSeries = new XYChart.Series();
        this.testNegativeSeries.setName("Test Negative");

        this.sc.getData().clear();
        this.sc.setAnimated(true);
    }
    /*
    载入测试数据
     */
    private void loadTestingFile(File file) throws IOException, UnknownDataFileException {
        testingData = dataFileFactory.loadTestingData(file);

        List<List<Double>> test = testingData.getTest();
        ObservableList<String> items = FXCollections.observableArrayList();

        if(!this.sc.getData().contains(this.testPositiveSeries)) {
            sc.getData().addAll(this.testPositiveSeries, this.testNegativeSeries);
        }

        for (List<Double> pos : test) {
            boolean positive = this.plotTestingData(pos);
            String str = String.format("%s\t%f; %f", positive ? "+" : "-", pos.get(0), pos.get(1));
            items.add(str);
        }

        this.testingDataList.setItems(items);
    }
    /*
    载入训练数据
     */
    private void loadTrainingFile(File file) throws IOException, UnknownDataFileException {
        trainingData = dataFileFactory.loadTrainingData(file);

        List<List<Double>> positive = trainingData.getPositive();
        List<List<Double>> negative = trainingData.getNegative();

        ObservableList<String> items = FXCollections.observableArrayList();

        for (List<Double> pos : positive) {
            String str = String.format("+\t%f; %f", pos.get(0), pos.get(1));
            items.add(str);
        }

        for (List<Double> neg : negative) {
            String str = String.format("-\t%f; %f", neg.get(0), neg.get(1));
            items.add(str);
        }

        this.trainingDataList.setItems(items);
        this.plotTrainingData(trainingData);
        this.stateTrainingLoaded();
    }
    /*
    描点:训练数据
    */
    private void plotTrainingData(TrainingData trainingData) throws IOException {
        this.sc.setAnimated(false);
        this.sc.setCreateSymbols(false);
        this.sc.getData().clear();

        XYChart.Series series1 = new XYChart.Series();
        series1.setName("Positive");

        XYChart.Series series2 = new XYChart.Series();
        series2.setName("Negative");

        FXMLLoader loader = new FXMLLoader(Main.class.getClassLoader().getResource("fxml/sample.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();

        for (List<Double> array : trainingData.getPositive()) {
            series1.getData().add(new XYChart.Data(array.get(0), array.get(1)));
        }

        for (List<Double> array : trainingData.getNegative()) {
            series2.getData().add(new XYChart.Data(array.get(0), array.get(1)));
        }

        sc.setCreateSymbols(true);
        sc.getData().addAll(series1, series2);
        sc.setAnimated(true);
    }
    /*
    描点:测试数据
     */
    private boolean plotTestingData(List<Double> data) throws IOException {
        RealVector t = this.singleDoubleToRealVector(data);
        Classification classification = svm.classify(t);

        if (classification.equals(Classification.POSITIVE)) {
            this.testPositiveSeries.getData().add(new XYChart.Data(t.toArray()[0], t.toArray()[1]));
            return true;
        } else {
            this.testNegativeSeries.getData().add(new XYChart.Data(t.toArray()[0], t.toArray()[1]));
            return false;
        }
    }
    /*
    训练
     */
    private void train(TrainingData trainingData, int precision) {
        ProgressIndicator pi = new ProgressIndicator();
        VBox box = new VBox(pi);
        box.setAlignment(Pos.CENTER);
        this.pane.setDisable(true);
        this.stackPane.getChildren().add(box);

        Thread one = new Thread(() -> {
            boolean found = false;
            List<RealVector> negative = this.doubleToRealVector(trainingData.getNegative());
            List<RealVector> positive = this.doubleToRealVector(trainingData.getPositive());
            try {
                this.svm.train(negative, positive, precision);
                found = true;
            } catch (SolutionNotFoundException e) {
            }

            boolean finalFound = found;
            Platform.runLater(() -> {
                if (finalFound) {
                    this.plotSvm();
                    this.stateTrained();
                } else {
                    this.showError("无解!");
                    this.stateTrainingLoaded();
                }

                pane.setDisable(false);
                this.stackPane.getChildren().remove(box);
            });

        });

        one.start();
    }
    /*
    画出分界线
     */
    private void plotSvm() {
        PointPair mainVector = this.svm.getMainVector();
        PointPair positiveVector = this.svm.getPositiveVector();
        PointPair negativeVector = this.svm.getNegativeVector();

        XYChart.Series series5 = new XYChart.Series();
        series5.setName("Vector");

        XYChart.Series series6 = new XYChart.Series();
        series6.setName("Vector+1");

        XYChart.Series series7 = new XYChart.Series();
        series7.setName("Vector-1");

        series5.getData().add(new XYChart.Data<>(mainVector.getFromX(), mainVector.getFromY()));
        series5.getData().add(new XYChart.Data<>(mainVector.getToX(), mainVector.getToY()));

        series6.getData().add(new XYChart.Data<>(positiveVector.getFromX(), positiveVector.getFromY()));
        series6.getData().add(new XYChart.Data<>(positiveVector.getToX(), positiveVector.getToY()));

        series7.getData().add(new XYChart.Data<>(negativeVector.getFromX(), negativeVector.getFromY()));
        series7.getData().add(new XYChart.Data<>(negativeVector.getToX(), negativeVector.getToY()));

        sc.getData().addAll(series5, series6, series7);
    }
    /*
    类型转换:List<List<Double>>转RealVector
     */
    private List<RealVector> doubleToRealVector(List<List<Double>> list) {
        List<RealVector> ret = new ArrayList<>();

        for (List<Double> arr : list) {
            ret.add(this.singleDoubleToRealVector(arr));
        }

        return ret;
    }
    /*
    类型转换:List<Double>转RealVector
     */
    private RealVector singleDoubleToRealVector(List<Double> list) {
        double[] doubles = new double[list.size()];

        for (int i = 0; i < list.size(); i++) {
            doubles[i] = list.get(i);
        }

        return new ArrayRealVector(doubles);

    }
    /*
    显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
