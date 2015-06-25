import numpy as np
from numpy import dot, zeros, eye, empty, loadtxt, ones
from numpy.linalg import inv
from commonFunctions import trace, squareFrobeniusNormOfSparse
from scipy.sparse import csr_matrix, dok_matrix

def updateA(X, A, R, V, D, lmbda):
    n, rank = A.shape
    F = zeros((n,rank), dtype=np.float64)
    E = zeros((rank, rank), dtype=np.float64)

    AtA = dot(A.T, A)
    for i in range(len(X)):
        ar = dot(A, R[i])
        art = dot(A, R[i].T)
        F += X[i].dot(art) + X[i].T.dot(ar)
        E += dot(R[i], dot(AtA, R[i].T)) + dot(R[i].T, dot(AtA, R[i]))
    A = dot(F  + D.dot(V.T), inv(lmbda * eye(rank) + E + dot(V, V.T)))
    return A

def updateV(A, D, lmbda):
    n, rank = A.shape    
    At = A.T
    invPart = empty((1, 1))
    if lmbda == 0:
        invPart = inv(dot(At, A))
    else :
        invPart = inv(dot(At, A) + lmbda * eye(rank))
    return dot(invPart, At) * D

def matrixFitNorm(D, A, V):
    """
    Computes the Frobenius norm of the fitting matrix ||D - A*V||,
    where D is a sparse matrix
    """ 
    return squareFrobeniusNormOfSparse(D) + matrixFitNormWithoutNormD(D, A, V)

def matrixFitNormWithoutNormD(D, A, V):
    thirdTerm = dot(dot(V, V.T), dot(A.T, A))
    secondTerm = dot(A.T, D.dot(V.T))
    return np.trace(thirdTerm) - 2 * trace(secondTerm) 

def loadD(inputDir, dim):
    extDim = 0
    with open('./%s/words' % inputDir) as words:
        for line in words:
            extDim += 1
    print 'The number of words: %d' % extDim
    
    extRow = loadtxt('./%s/ext-matrix-rows' % inputDir, dtype=np.uint32)
    if extRow.size == 1: 
        extRow = np.atleast_1d(extRow)
    extCol = loadtxt('./%s/ext-matrix-cols' % inputDir, dtype=np.uint32)
    if extCol.size == 1: 
        extCol = np.atleast_1d(extCol)
    
    print 'The number of non-zero values in the additional matrix: %d' % extRow.size
    
    return csr_matrix((ones(extRow.size),(extRow,extCol)), shape=(dim,extDim))

def loadDfloat(inputDir, dim):
    extDim = 0
    with open('./%s/words' % inputDir) as words:
        for line in words:
            extDim += 1
    print 'The number of words: %d' % extDim
    
    extRow = loadtxt('./%s/ext-matrix-rows' % inputDir, dtype=np.uint32)
    if extRow.size == 1: 
        extRow = np.atleast_1d(extRow)
    extCol = loadtxt('./%s/ext-matrix-cols' % inputDir, dtype=np.uint32)
    if extCol.size == 1: 
        extCol = np.atleast_1d(extCol)
    extVal = loadtxt('./%s/ext-matrix-elements' % inputDir, dtype=np.float32)
    if extVal.size == 1: 
        extVal = np.atleast_1d(extVal)
            
    D = dok_matrix((dim,extDim), dtype=np.float32)
    for i in xrange(extVal.size):
        D[extRow[i], extCol[i]] = extVal[i]
        
    print 'The number of non-zero values in the additional matrix: %d' % extRow.size
                
    return D.tocsr()


    
