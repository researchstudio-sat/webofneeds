import argparse
from numpy.linalg import pinv
from numpy import loadtxt, savetxt

parser = argparse.ArgumentParser()
parser.add_argument("--input", type=str, help="the file where the m x n matrix is stored",
                     required=True)
parser.add_argument("--output", type=str, help="the file where the pseudo matrix of the transposed input matrix will be output",
                     required=True)
args = parser.parse_args()
output = args.output
Vt = loadtxt(open(args.input, "rb"), delimiter=" ") 

savetxt(output, pinv(Vt.T))
print 'Done.'