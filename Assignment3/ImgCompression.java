package MyImage;

import static java.lang.Math.cos;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class ImgCompression {
	// ����width��height�ֱ�Ϊ512
	private static int imgWith = 512;
	private static int imgHeight = 512;

	// �ֱ�ΪԭʼͼƬ��dtc��dwt ͼ�񴴽� ����͸��ɫ�Ķ���
	BufferedImage OrgImage = new BufferedImage(imgWith, imgHeight,
			BufferedImage.TYPE_INT_RGB);
	BufferedImage DctImage = new BufferedImage(imgWith, imgHeight,
			BufferedImage.TYPE_INT_RGB);;
	BufferedImage DwtImage = new BufferedImage(imgWith, imgHeight,
			BufferedImage.TYPE_INT_RGB);;

	// ����һ��8*8�Ŀ�
	static double[][] Matrixblock = new double[8][8];
	// Matrix for original R, G, B
	int[][] R_Matrix = new int[imgHeight][imgWith];
	int[][] G_Matrix = new int[imgHeight][imgWith];
	int[][] B_Matrix = new int[imgHeight][imgWith];

	// DCT ���� R, G, B��
	int[][] DCT_R_Matrix = new int[imgHeight][imgWith];
	int[][] DCT_G_Matrix = new int[imgHeight][imgWith];
	int[][] DCT_B_Matrix = new int[imgHeight][imgWith];

	//  IDCT���� (R, G, B)
	int[][] IDCT_R_Matrix = new int[imgHeight][imgWith];
	int[][] IDCT_G_Matrix = new int[imgHeight][imgWith];
	int[][] IDCT_B_Matrix = new int[imgHeight][imgWith];

	// DWT����  (R, G, B)
	double[][] DWT_R_Matrix = new double[imgHeight][imgWith];
	double[][] DWT_G_Matrix = new double[imgHeight][imgWith];
	double[][] DWT_B_Matrix = new double[imgHeight][imgWith];

	//IDWT���� R, G, B
	int[][] IDWT_R_Matrix = new int[imgHeight][imgWith];
	int[][] IDWT_G_Matrix = new int[imgHeight][imgWith];
	int[][] IDWT_B_Matrix = new int[imgHeight][imgWith];

	public static void main(String[] args) {

		ImgCompression ren = new ImgCompression();
		ren.showImage(args);
	}

	/**
	 * ��ȡԭʼͼ�񲢰����㷨����DCt��DWT����
	 */
	public void showImage(String[] args) {

		try {
			InputStream is = new FileInputStream(new File(args[0]));
			byte[] bytes = new byte[(int) new File(args[0]).length()];
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
			Calculateoffset(bytes);
			CalculateInitialCosineTransform(args);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * ����ƫ����
	 */
	private void Calculateoffset(byte[] size) {
		// ����ƫ����
		int ind = 0;
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				int r = size[ind];
				int g = size[ind + imgHeight * imgWith];
				int b = size[ind + imgHeight * imgWith * 2];

				// ת��Ϊunsigned int
				r = r & 0xFF;
				g = g & 0xFF;
				b = b & 0xFF;

				// �洢ԭʼ��R��G��Bֵ
				R_Matrix[y][x] = r;
				G_Matrix[y][x] = g;
				B_Matrix[y][x] = b;

				int pixOriginal = 0xff000000 | ((r & 0xff) << 16)
						| ((g & 0xff) << 8) | (b & 0xff);
				OrgImage.setRGB(x, y, pixOriginal);
				ind++;
				x++;
			}
			y++;
		}

	}

	/**
	 * ��������еĳ�ʼ���ұ任ֵ
	 */
	private void CalculateInitialCosineTransform(String[] args) {

		int p = 0;
		while (p < 8) {
			int q = 0;
			while (q < 8) {
				Matrixblock[p][q] = cos((2 * p + 1) * q * 3.14159 / 16.00);
				q++;
			}
			p++;
		}
		int n = Integer.parseInt(args[1]);
		if (n != -1) {
			int m = n / 4096;
			// ��ɢ���ұ任��DCT��������DCT��Zig-Zag����
			dctQuantize(R_Matrix, G_Matrix, B_Matrix, m);
			// IDCT
			CalculationReverseDCT();

			// ��ɢС���任��DWT��������DWT��Zig-Zag����
			// ����
			DWT_R_Matrix = dwtStandardDecomposition(R_Matrix, n);
			DWT_G_Matrix = dwtStandardDecomposition(G_Matrix, n);
			DWT_B_Matrix = dwtStandardDecomposition(B_Matrix, n);

			IDWT_R_Matrix = idwtComposition(DWT_R_Matrix);
			IDWT_G_Matrix = idwtComposition(DWT_G_Matrix);
			IDWT_B_Matrix = idwtComposition(DWT_B_Matrix);

			// ��ʾDCT��DWTͼ��
			displayDctDwtImage(0);
			displayImg(0);
		} else {
			int diedai = 1;
			int i = 4096;
			int res = 512 * 512;
			while (i < res) {
				p = i;
				int k = p / 4096;
				// ��ɢ���ұ任��DCT��������DCT��Zig-Zag����
				dctQuantize(R_Matrix, G_Matrix, B_Matrix, k);
				// IDCT
				CalculationReverseDCT();
				// ��ɢС���任��DWT��������DWT��Zig-Zag����
				DWT_R_Matrix = dwtStandardDecomposition(R_Matrix, p);
				DWT_G_Matrix = dwtStandardDecomposition(G_Matrix, p);
				DWT_B_Matrix = dwtStandardDecomposition(B_Matrix, p);

				IDWT_R_Matrix = idwtComposition(DWT_R_Matrix);
				IDWT_G_Matrix = idwtComposition(DWT_G_Matrix);
				IDWT_B_Matrix = idwtComposition(DWT_B_Matrix);
				// ��1����ӳ���ʾDCT��DWTͼ��
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				displayDctDwtImage(diedai);
				displayImg(diedai);
				diedai++;

				if (i == res) { // ѭ��
					i = 0;
					diedai = 1;
				}
				i = i + 4096;
			}
		}

	}

	/**
	 * ����ѹ���㷨������ʾDCT��DWTͼ��
	 */
	private void displayDctDwtImage(int iteration) {
		// ��ʾͼ�����
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				// ��ʾIDCT����
				int r = IDCT_R_Matrix[y][x];
				int g = IDCT_G_Matrix[y][x];
				int b = IDCT_B_Matrix[y][x];
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
						| (b & 0xff);
				DctImage.setRGB(x, y, pix);
				// ��ʾIDwT����
				int rr = (int) IDWT_R_Matrix[y][x];
				int gg = (int) IDWT_G_Matrix[y][x];
				int bb = (int) IDWT_B_Matrix[y][x];

				int pixx = 0xff000000 | ((rr & 0xff) << 16)
						| ((gg & 0xff) << 8) | (bb & 0xff);
				DwtImage.setRGB(x, y, pixx);
				x++;
			}
			y++;
		}
	}

	// ��ʾͼ��
	public void displayImg(int iteration) {
		JFrame jFrame = new JFrame();
		GridBagLayout gridBagLayout = new GridBagLayout();
		JLabel jLabel_text1 = new JLabel();
		JLabel jLabel_text2 = new JLabel();
		JLabel jLabel_img1 = new JLabel();
		JLabel jLabel_img2 = new JLabel();

		jFrame.getContentPane().setLayout(gridBagLayout);
		jLabel_text1.setText(iteration != 0 ? "DCT (Iteration : " + iteration
				+ ")" : "DCT");
		jLabel_text1.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel_text2.setText(iteration != 0 ? "DWT (Iteration : " + iteration
				+ ")" : "DWT");
		jLabel_text2.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel_img1.setIcon(new ImageIcon(DctImage));
		jLabel_img2.setIcon(new ImageIcon(DwtImage));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		jFrame.getContentPane().add(jLabel_text1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		jFrame.getContentPane().add(jLabel_text2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		jFrame.getContentPane().add(jLabel_img1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		jFrame.getContentPane().add(jLabel_img2, c);

		jFrame.pack();
		jFrame.setVisible(true);
	}

	/**
	 * ��������ת��
	 */
	private double[][] transpose(double[][] matrix) {
		double[][] result = new double[imgHeight][imgWith];
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				result[y][x] = matrix[x][y];
				x++;
			}
			y++;
		}
		return result;
	}

	/**
	 * DWT��׼�ֽ⹦��
	 */
	private double[][] dwtStandardDecomposition(int[][] matrix, int n) {
		// ���Ƶ�˫����
		double[][] dMatrix = new double[imgHeight][imgWith];
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				dMatrix[y][x] = matrix[y][x];
				x++;
			}
			y++;

		}

		// ����������ǰ
		int r = 0;
		while (r < imgWith) {
			dMatrix[r] = decomposition(dMatrix[r]);
			r++;
		}
		// �������ֺ�
		int c = 0;
		dMatrix = transpose(dMatrix);
		while (c < imgHeight) {
			dMatrix[c] = decomposition(dMatrix[c]);
			c++;
		}
		dMatrix = transpose(dMatrix);
		// ���о���α���
		dMatrix = ZagTraver(dMatrix, n);
		return dMatrix;
	}

	private double[] decomposition(double[] array) {
		int h = array.length;
		while (h > 0) {
			array = decompositionStep(array, h);
			h = h / 2;
		}
		return array;
	}

	private double[] decompositionStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		int k = 0;
		while (k < h / 2) {
			dArray[k] = (array[2 * k] + array[2 * k + 1]) / 2; // ��ͨ
			dArray[h / 2 + k] = (array[2 * k] - array[2 * k + 1]) / 2; // ��ͨ
			k++;
		}
		return dArray;
	}

	/**
	 * IDWT�㷨ִ��DWT�ķ�����������´���ͼ��
	 */
	private int[][] idwtComposition(double[][] matrix) {
		int[][] iMatrix = new int[imgHeight][imgWith];

		// ��һ��
		matrix = transpose(matrix);
		for (int col = 0; col < imgHeight; col++) {
			matrix[col] = composition(matrix[col]);
		}
		matrix = transpose(matrix);
		// ������
		for (int row = 0; row < imgWith; row++) {
			matrix[row] = composition(matrix[row]);
		}

		// ���ɷ־����Ƶ�R��G��Bֵ��int���� ֵ��0-255
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				iMatrix[y][x] = (int) Math.round(matrix[y][x]);
				if (iMatrix[y][x] < 0) {
					iMatrix[y][x] = 0;
				}
				if (iMatrix[y][x] > 255) {
					iMatrix[y][x] = 255;
				}
				x++;
			}
			y++;
		}
		return iMatrix;
	}

	private double[] composition(double[] array) {
		int h = 1;
		while (h <= array.length) {
			array = compositionStep(array, h);
			h = h * 2;
		}
		return array;
	}

	private double[] compositionStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		for (int i = 0; i < h / 2; i++) {
			dArray[2 * i] = array[i] + array[h / 2 + i];
			dArray[2 * i + 1] = array[i] - array[h / 2 + i];
		}
		return dArray;
	}

	/**
	 * ����ѹ���㷨����DCTת��
	 */
	private void dctQuantize(int[][] R_Matrix, int[][] G_Matrix, int[][] B_Matrix,
			int m) {
		int temp = 8;
		int q = 0;
		while (q < imgHeight) {
			int w = 0;
			while (w < imgWith) {
				double[][] rBlock = new double[8][8];
				double[][] gBlock = new double[8][8];
				double[][] bBlock = new double[8][8];

				int e = 0;
				while (e < temp) {
					int r = 0;
					while (r < temp) {

						float cu = 1.0f, cv = 1.0f;
						float Rtemp = 0.00f, Gtemp = 0.00f, Btemp = 0.00f;
						if (e == 0)
							cu = 0.707f;
						if (r == 0)
							cv = 0.707f;
						int t = 0;
						while (t < temp) {
							int y = 0;
							while (y < temp) {

								double tempR, tempG, tempB;

								tempR = (int) R_Matrix[q + t][w + y];
								tempG = (int) G_Matrix[q + t][w + y];
								tempB = (int) B_Matrix[q + t][w + y];

								// IDCT��ʽ����
								Rtemp += tempR * Matrixblock[t][e]
										* Matrixblock[y][r];
								Gtemp += tempG * Matrixblock[t][e]
										* Matrixblock[y][r];
								Btemp += tempB * Matrixblock[t][e]
										* Matrixblock[y][r];
								y++;
							}
							t++;
						}
						rBlock[e][r] = (int) Math.round(Rtemp * 0.25 * cu * cv);
						gBlock[e][r] = (int) Math.round(Gtemp * 0.25 * cu * cv);
						bBlock[e][r] = (int) Math.round(Btemp * 0.25 * cu * cv);
						r++;
					}
					e++;
				}
				// ���о���α���
				rBlock = ZagTraver(rBlock, m);
				gBlock = ZagTraver(gBlock, m);
				bBlock = ZagTraver(bBlock, m);
				for (int u = 0; u < 8; u++) {
					for (int v = 0; v < 8; v++) {
						DCT_R_Matrix[q + u][w + v] = (int) rBlock[u][v];
						DCT_G_Matrix[q + u][w + v] = (int) gBlock[u][v];
						DCT_B_Matrix[q + u][w + v] = (int) bBlock[u][v];
					}// end v
				}// end u

				w += 8;
			}
			q += 8;
		}
	}

	/**
	 * ���۾�������Լ���ϵ��
	 */
	public double[][] ZagTraver(double[][] matrix, int m) {
		int i = 0;
		int j = 0;
		int length = matrix.length - 1;
		int count = 1;

		// ���ھ����������
		if (count > m) {
			matrix[i][j] = 0;
			count++;
		} else {
			count++;
		}

		while (true) {

			j++;
			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (j != 0) {
				i++;
				j--;

				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
			i++;
			if (i > length) {
				i--;
				break;
			}

			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (i != 0) {
				i--;
				j++;
				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
		}

		// ���ھ����������
		while (true) {
			j++;
			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (j != length) {
				j++;
				i--;

				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
			i++;
			if (i > length) {
				i--;
				break;
			}

			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (i != length) {
				i++;
				j--;
				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
		}
		return matrix;
	}

	/**
	 * ���ݹ�ʽ������DCT�任���м���
	 */
	public void CalculationReverseDCT() {
		int temp = 8;
		int q = 0;
		while (q < imgHeight) {
			int w = 0;
			while (w < imgWith) {
				int e = 0;
				while (e < temp) {
					int r = 0;
					while (r < temp) {
						float Rtemp = 0.00f, Gtemp = 0.00f, Btemp = 0.00f;
						int t = 0;
						while (t < temp) {
							int y = 0;
							while (y < temp) {
								float temp_t = 1.0f, temp_y = 1.0f;
								if (t == 0)
									temp_t = 0.707f;
								if (y == 0)
									temp_y = 0.707f;
								double tempR, tempG, tempB;
								tempR = DCT_R_Matrix[q + t][w + y];
								tempG = DCT_G_Matrix[q + t][w + y];
								tempB = DCT_B_Matrix[q + t][w + y];
								// IDCT Formula calculation
								Rtemp += temp_t * temp_y * tempR
										* Matrixblock[e][t] * Matrixblock[r][y];
								Gtemp += temp_t * temp_y * tempG
										* Matrixblock[e][t] * Matrixblock[r][y];
								Btemp += temp_t * temp_y * tempB
										* Matrixblock[e][t] * Matrixblock[r][y];
								y++;
							}
							t++;
						}

						Rtemp *= 0.25;
						Gtemp *= 0.25;
						Btemp *= 0.25;

						// ���R��G��Bֵ�Ƿ������������������0��255֮��
						if (Rtemp <= 0)
							Rtemp = 0;
						else if (Rtemp >= 255)
							Rtemp = 255;

						if (Gtemp <= 0)
							Gtemp = 0;
						else if (Gtemp >= 255)
							Gtemp = 255;
						if (Btemp <= 0)
							Btemp = 0;
						else if (Btemp >= 255)
							Btemp = 255;

						IDCT_R_Matrix[q + e][w + r] = (int) Rtemp;
						IDCT_G_Matrix[q + e][w + r] = (int) Gtemp;
						IDCT_B_Matrix[q + e][w + r] = (int) Btemp;
						r++;
					}
					e++;
				}
				w += 8;
			}
			q += 8;
		}

	}

}
