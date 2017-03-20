from scipy.sparse import coo_matrix
from numpy import ones, dot, eye
import numpy as np
from extrescalFunctions import updateA, updateV, matrixFitNorm, matrixFitNormWithoutNormD
from commonFunctions import squareFrobeniusNormOfSparse
from nose.tools import assert_almost_equal
from numpy.linalg import inv
from numpy.linalg.linalg import norm

def testUpdateA():
    A = np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1]])
    D = coo_matrix((ones(6),([0, 1, 2, 3, 4, 5], [0, 1, 1, 2, 3, 3])), shape=(6, 4), dtype=np.uint8).tocsr()
    V = np.array([[0.1, 0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1, 0.1]])
    R = []
    R.append(np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1]]))
    R.append(np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1]]))
    R.append(np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1]]))
    
    X = []
    X.append(coo_matrix((ones(2),([1, 2], [0, 0])), shape=(6, 6), dtype=np.uint8).tolil())
    X.append(coo_matrix((ones(1),([0], [3])), shape=(6, 6), dtype=np.uint8).tolil())
    X.append(coo_matrix((ones(3),([4, 5, 5], [4, 4, 5])), shape=(6, 6), dtype=np.uint8).tolil())
    lmbda = 0.001
    
    F = D.dot(V.T) + X[0].dot(dot(A, R[0].T)) + X[1].dot(dot(A, R[1].T)) + X[2].dot(dot(A, R[2].T)) + X[0].T.dot(dot(A, R[0])) + X[1].T.dot(dot(A, R[1])) + X[2].T.dot(dot(A, R[2]))
    S = dot(V, V.T) + dot(dot(dot(R[0], A.T), A), R[0].T) + dot(dot(dot(R[1], A.T), A), R[1].T) + dot(dot(dot(R[2], A.T), A), R[2].T) + dot(dot(dot(R[0].T, A.T), A), R[0]) + dot(dot(dot(R[1].T, A.T), A), R[1]) + dot(dot(dot(R[2].T, A.T), A), R[2]) + lmbda*eye(3)
    expectedNewA = dot(F, inv(S))
     
    newA = updateA(X, A, R, V, D, lmbda)
    for i in range(6):
        for j in range(3):
            assert_almost_equal(newA[i,j], expectedNewA[i, j])
            
def testUpdateV():
    A = np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1]])
    V = np.array([[0.1, 0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1, 0.1],
         [0.1, 0.1, 0.1, 0.1]])
    lmbda = 0.0001
    D = coo_matrix((ones(6),([0, 1, 2, 3, 4, 5], [0, 1, 1, 2, 3, 3])), shape=(6, 4), dtype=np.uint8).tocsr()
    expectedNewV = dot(dot(inv(dot(A.T, A) + lmbda*eye(3)), A.T), D.todense())
    newV = updateV(A, D, lmbda)
    for i in range(3):
        for j in range(4):
            assert_almost_equal(newV[i,j], expectedNewV[i, j])

def testMatrixFitNorm():
    A = np.array([[0.1, 0.1, 0.1],
         [0.1, 0.1, 0.001],
         [0.2, 0.1, 0.1],
         [0.1, 0.3, 0.1],
         [0.4, 0.1, 0.1],
         [0.001, 0.01, 0.1]])
    V = np.array([[0.1, 0.4, 0.1, 0.1],
         [0.01, 0.3, 0.1, 0.3],
         [0.1, 0.01, 0.4, 0.001]])
    D = coo_matrix((ones(6),([0, 1, 2, 3, 4, 5], [0, 1, 1, 2, 3, 3])), shape=(6, 4), dtype=np.uint8).tocsr()
    expectedNorm = norm(D - dot(A,V))**2
    assert_almost_equal(matrixFitNorm(D, A, V), expectedNorm)
    assert_almost_equal(squareFrobeniusNormOfSparse(D) + matrixFitNormWithoutNormD(D, A, V), expectedNorm)
        
    