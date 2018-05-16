package AnnaSkywalker.svm;


import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;

/**
 * SVM
 */
public class SupportVectorMachine {
    /*
    y(x)=wx+b
     */
    private RealVector w;
    private double b;
    private double min;
    private double max;

    private static final int DEFAULT_PRECISION = 4;
    private static final double RANGE_MULTIPLE = 5;
    private static final double B_MULTIPLE = 5;
    private final static double[][] TRANSFORMS = {
            {1, 1},
            {-1, 1},
            {-1, -1},
            {1, -1},
    };
    /*
    获取向量
    注意,这个函数的调用点是训练完毕开始绘图的时候,
    坐标系是有限的,并不能真的画出直线,我们只能画线段来表示,
    为了效果理想,最好线段的两个端点在全部点的外侧,这样看起来不会觉得线太短
     */
    private PointPair getVector(int multiple) {
        if(this.w == null) {
            return null;
        }

        double min = this.min * 0.9;
        double max = this.max * 1.1;

        return new PointPair(min, this.hyperplane(min, multiple), max, this.hyperplane(max, multiple));
    }

    public PointPair getMainVector() {
        return this.getVector(0);
    }

    public PointPair getPositiveVector() {
        return this.getVector(1);
    }

    public PointPair getNegativeVector() {
        return this.getVector(-1);
    }
    /*
    输入:double x:x坐标
        int    v:(-1,0,1),代表那三条线,取0的时候获得的是分界线
        返回:对应直线(超平面)上的y坐标
     */
    private double hyperplane(double x, int v) {
        return (-this.w.getEntry(0) * x - this.b + v) / this.w.getEntry(1);
    }
    /*
    训练入口
    参数:负样本:List<RealVector>
        正样本;List<RealVector>
        精度
     */
    public void train(List<RealVector> negative, List<RealVector> positive, int precisionSteps) throws SolutionNotFoundException {
        precisionSteps = precisionSteps < 0 ? DEFAULT_PRECISION : precisionSteps;
        HashMap<Double, Option> options = new LinkedHashMap<>();
        List<RealVector> merged = new ArrayList<>(negative);
        merged.addAll(positive);//所有样本点合并在一起

        this.max = this.findMax(merged);//找到样本坐标值中的最大值和最小值
        this.min = this.findMin(merged);

        Double[] stepSizes = this.generateStepSizes(precisionSteps, this.max);//得到步长数组

        double previousOptimum = this.max * 10;
        boolean isOptimized;

        for (double step : stepSizes) {
            RealVector wTest = new ArrayRealVector(new double[]{previousOptimum, previousOptimum});
            isOptimized = false;

            while (!isOptimized) {
                findOption(negative, positive, options, step, wTest);

                if (wTest.getEntry(0) < 0) {
                    isOptimized = true;
                } else {
                    wTest = this.addToVector(wTest, -step);
                }
            }

            Option minimalOption = this.getMinimalOption(options);
            this.w = minimalOption.getW();
            this.b = minimalOption.getB();
            previousOptimum = this.w.getEntry(0) + step * 2;
        }
    }

    private void findOption(List<RealVector> negative, List<RealVector> positive, HashMap<Double, Option> options, double step, RealVector wTest) {
        List<Double> steps = this.stepper(
                -1 * (this.max * RANGE_MULTIPLE),
                this.max * RANGE_MULTIPLE,
                step * B_MULTIPLE
        );

        for (double singleStep : steps) {
            for (double[] transformation : TRANSFORMS) {
                RealVector wTestScaled = this.vectorScale(wTest, transformation);
                boolean validOption = true;

                for (RealVector neg : negative) {
                    if (!tryFit(wTestScaled, neg, singleStep, -1)) {
                        validOption = false;
                        break;
                    }
                }

                if (validOption) {
                    for (RealVector pos : positive) {
                        if (!tryFit(wTestScaled, pos, singleStep, 1)) {
                            validOption = false;
                            break;
                        }
                    }
                }

                if (validOption) {
                    Option item = new Option(wTestScaled, singleStep);
                    double norm = wTestScaled.getNorm();
                    options.put(norm, item);
                }
            }
        }
    }

    private boolean tryFit(RealVector wTest, RealVector vector, double margin, int scale) {
        return scale * (wTest.dotProduct(vector) + margin) >= 1;
    }
    /*
    计算步长数组
    步长数组的大小是precisionSteps
    步长越来越小
     */
    private Double[] generateStepSizes(int precisionSteps, double max) {
        Double[] stepSizes = new Double[precisionSteps];

        for (int i = 1; i <= precisionSteps; i++) {
            stepSizes[i - 1] = Math.pow(0.1, i) * max;
        }

        return stepSizes;
    }
    /*
     返回List<RealVector>中的最小坐标值
     */
    private double findMin(List<RealVector> merged) {
        double min = Double.MAX_VALUE;

        for (RealVector vector : merged) {
            for (int i = 0; i < vector.getDimension(); i++) {
                if (vector.getEntry(i) < min) {
                    min = vector.getEntry(i);
                }
            }
        }

        return min;
    }
   /*
    返回List<RealVector>中的最大坐标值
    */
    private double findMax(List<RealVector> merged) {
        double max = Double.MIN_VALUE;

        for (RealVector vector : merged) {
            for (int i = 0; i < vector.getDimension(); i++) {
                if (vector.getEntry(i) > max) {
                    max = vector.getEntry(i);
                }
            }
        }

        return max;
    }

    private Option getMinimalOption(HashMap<Double, Option> options) throws SolutionNotFoundException {
        if (options.size() == 0) {
            throw new SolutionNotFoundException();
        }

        Double min = new ArrayList<>(options.keySet()).get(0);
        for (Double key : options.keySet()) {

            if (key < min) {
                min = key;
            }
        }

        return options.get(min);
    }

    private RealVector addToVector(RealVector w, double v) {
        RealVector ret = new ArrayRealVector(w.getDimension());

        for (int i = 0; i < w.getDimension(); i++) {
            ret.setEntry(i, w.getEntry(i) + v);
        }

        return ret;
    }

    private List<Double> stepper(double from, double to, double step) {
        List<Double> ret = new ArrayList<>();

        for (double i = from; i <= to; i += step) {
            ret.add(i);
        }

        return ret;
    }

    private RealVector vectorScale(RealVector vector, double[] scale) {
        RealVector ret = new ArrayRealVector(vector.getDimension());

        for (int i = 0; i < vector.getDimension(); i++) {
            ret.setEntry(i, vector.getEntry(i) * scale[i]);
        }

        return ret;
    }

    public Classification classify(RealVector feature) {
        if(this.w == null) {
            return null;
        }

        double result = feature.dotProduct(this.w) + this.b;
        return result < 0 ? Classification.NEGATIVE : Classification.POSITIVE;
    }
}
