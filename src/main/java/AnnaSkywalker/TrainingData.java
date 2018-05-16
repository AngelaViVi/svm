package AnnaSkywalker;

import java.util.List;

/**
 * 训练数据
 */
public class TrainingData {
    private List<List<Double>> positive;//正样本
    private List<List<Double>> negative;//负样本

    public TrainingData(List<List<Double>> positive, List<List<Double>> negative){
        this.positive = positive;
        this.negative = negative;
    }

    public List<List<Double>> getPositive() {
        return positive;
    }

    public List<List<Double>> getNegative() {
        return negative;
    }
}
