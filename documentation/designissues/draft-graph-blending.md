# Blending:
## Definition
Given two RDF graphs g1 and g2, blending refers to the pairwise renaming 
of resources (blank nodes or URI nodes) r1 in g1 and r2 in g2 such that 
one resource name (which may be r1, r2, or any other URI) is used 
instead of r1 and r2. The nodes r1 and r2 are said to be blended (ideas 
for a better term, anyone?). The resulting graphs are merged (avoiding 
accidental blank node identification, see RDF 1.1 Semantics[2]). The 
blending function b maps the input graphs to the set of all possible 
output graphs.

## Discussion
There are many ways to blend two graphs, and none of these ways are 
deemed correct or incorrect.
We require this operation to combine two data structures held by 
mutually independent agents that have certain expectations about the 
blending solution. These expectations divide the set of possible 
outcomes at least in acceptable and unacceptable ones, possibly even in 
a preference ranking. I'll try to formulate that as well in the 
following.


# Node blendability
## Rationale
One way of constraining the result is to restrict which nodes can be 
blended. So far, we have identified three types: variable nodes, 
constant nodes, and unblendable nodes. Each node in an RDF graph has 
exactly one of the three types. A variable node may be blended and its 
name may be changed. A constant node may also be blended, but its name 
is not allowed to change. An unblendable node must not be blended.

One may note that blending g1 and g2 is equivalent to merging them if no 
variable nodes are present in either graph.

It is currently unclear how the constant/variable/unblendable property 
should be expressed. My intuition is that in most practical use cases, 
most nodes atom to be unblendable (ontology/TBox resources, properties 
in general), therefore it would be more economical to assume nodes to be 
unblendable by default. Then, variable/constant nodes have to be 
annotated as such explicitly.
Maybe it would be an option to make definitions by URI prefix, so we can 
define a complete vocabulary as unblendable with one statement. 
(opinions on that?)

## Definition
Given two graphs g1 and g2, a blendability-aware blending function is a 
blending function for g1 and g2 that only replaces resources that have 
not been declared as constant with respect to blending. The latter is 
done by specifying a triple "<graph> bl:containsConstant <resource> ." 
or "<graph> bl:containsVariable <resource> ." in an additional graph 
passed to the blendability-aware blending function. Such a graph is 
called a 'blendability graph'.


# Constraints on the blending solution:
## Rationale
As explained earlier, there may be multiple parties that have 
expectations for the blending solution. So far, we have talked about the 
originators of the data. There may also be one or more intended 
consumers of the data. Nothing prevents the expectations from being 
mutually incompatible (meaning that there exists no graph that satisfies 
them all) or that they are even unsatisfiable by themselves. It would of 
course be beneficial if such cases could be detected automaticall, but 
for now, let's just leave it at noting the possibility. Now, if the 
information the agents provided, when considered in total, has spurious 
or conflicting information, or if information is missing (taking their 
respective expectations as a definition of what is acceptable), the 
blending solution cannot meet the expectations. Nevertheless, it has to 
be computed, in order to be able to take the next step and "repair" 
(better word, please) one or both graphs and try again.

It would be nice to use constraints for the following goals during the 
calculation of the blending solution:
1. pruning wholly unacceptable solutions
2. ranking unacceptable solutions by badness
3. ranking acceptable-looking, but incomplete solutions ("so far so 
good")

Constraints should allow us to identify an acceptable result, or to 
select the best result of a number of unacceptable ones, and report them 
to users so that they can repair them.

However, if the expectations define an acceptable result, anything else, 
however close, is unacceptable, and in the absence of acceptable results 
we are left with unacceptable ones, some of which may be completely 
bogus while others are almost acceptable. So, the three categories above 
cannot be separated, and the best we can hope for is reasonable ranking 
of unacceptable solutions.

For the conceptualization, we go with a simple SHACL-based approach. 
Other systems (SPARQL, ShEx) are definitely possible as well.

## Definition
The constraints-aware blending function is a blending function for two 
RDF graphs g1 and g2 and takes optional graph names s1,...,sN that 
denote graphs which contain SHACL shapes constraining the result. Each 
blending solution is validated using s1,...,sN; the result of the 
function is a mapping of the blending solutions to their respective 
SHACL validation results, indexed by shape graph name (s1,...,sN). A 
blending solution is called accepted with respect to a constraints graph 
s if it causes no SHACL validation results when validated against s. A 
blending solution is called accepted if it is accepted with respect to 
all specified shapes graphs.

## Discussion
The main issue in this step seems to be the pruning and ordering of 
unacceptable results. Blending functions may choose to drop blending 
solutions if they are deemed too bad - but where should they draw the 
line and still report an unacceptable blending solution while dropping 
another?

With respect to one shapes graph, we could define that validation result 
graphs are ranked by maximum severity (1 sh:Violation is worse than 1000 
sh:Warnings), and within those classes by number of validation results 
(2 Violations and 1 Warnings is worse than 1 Violation and 1000 
Warnings).

I'm suspecting that the validation results should allow us to prune at 
least some bad blending solutions if their validation results are 
supersets of other solutions' validation results: if solution A has 
validation results (R1) and solution B has validation result (R1, R2) we 
can say that solution B is a deterioration of solution A and should not 
be reported.

In addition to constraint-based pruning and ranking, it is of course 
possible to perform reasoning-based pruning and validate the blending 
solution according to appropriate reasoning logics (RDFS, OWL, ...). 
However, I am not sure as to the usefulness of this approach if no 
solution is found that is consistent according to the logic.


# Graph blending algorithms
Let's assume for this section that we are blending two graphs g1 and g2, 
and we know we have variable nodes var(g1) and constant nodes const(g1) 
in g1, and var(g2) and const(g2) in g2.

## Complete
The complete algorithm consists of enumerating all possible solutions
For each n in var(g1), there are the following solutions:
* 1 solution in which n is not blended at all
* 1 solution for each m in var(g2), in which n is blended with m
* 1 solution for each m in const(g2), in which n is blended with m
symmetrically the same is true for each n in var(g2).
Removal of duplicate solutions yields the set of solutions.

## Heuristic
We may interpret blending as a heuristic search and apply an A*-like 
algorithm to finding good results, assessing the distance to the goal by 
evaluating the constraints, and the distance from the start by the 
number of node blendings.

## Knowledge-based
Exploiting knowledge about the actual structures in g1 and g2, the 
heuristic approach could be enhanced to apply more appropriate distance 
estimates and select more promising paths first.


# Links:
1. https://lists.w3.org/Archives/Public/semantic-web/2017Nov/0067.html
2. https://www.w3.org/TR/rdf11-mt/
