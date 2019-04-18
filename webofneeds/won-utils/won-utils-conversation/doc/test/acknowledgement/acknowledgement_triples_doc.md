## oneLocalMessageFailsLocallyTest
**input**: all-messages-acknowledged.trig
**output**: all-messages-acknowledged.trig
**test name**: oneLocalMessageFailsLocallyTest

**diff**:

---------

## oneLocalMessageFailsLocallyTest
**input**: one-local-message-fails-locally.trig
**output**: one-local-message-fails-locally.trig
**test name**: oneLocalMessageFailsLocallyTest

**diff**: 

```
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
< }
< 
```

***diff** with all-messages-acknowledged.trig sample*:

```
 event:49hcv8na1594ijbpx3hp
            a                            msg:FromSystem ;
            msg:correspondingRemoteMessage
                    event:pi2jpw9a1q00d11kr9ez ;
            msg:messageType           msg:SuccessResponse ;
            msg:previousMessage       event:8950tg6pjze6lr52pq3y , event:rrmkri9cdrtvp1bkjcnl ;
            msg:receivedTimestamp     1513170819409 ;
            msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
            msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
            msg:recipientNode          <https://localhost:8443/won/resource> ;
            msg:sender                conn:b4vtw60q5p3ro3yfjybs ;
            msg:senderAtom            atom:2615528351738345500 ;
            msg:senderNode            <https://localhost:8443/won/resource> ;
            msg:isRemoteResponseTo       event:eczqg8lp7xbukpzikd41 ;

event:ofx1afjv35cwpppp0wyg
            a                            msg:FromSystem ;
            msg:messageType           msg:SuccessResponse ;
            msg:previousMessage       event:ck5071fsyaned6upryxj , event:eczqg8lp7xbukpzikd41 ;
            msg:receivedTimestamp     1513170818746 ;
            msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
            msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
            msg:recipientNode          <https://localhost:8443/won/resource> ;
            msg:sender                conn:lhrkbkodc0y4rmc7z51y ;
            msg:senderAtom            atom:ekdwt2a60tesl77r13mu ;
            msg:senderNode            <https://localhost:8443/won/resource> ;
            msg:isResponseTo             event:eczqg8lp7xbukpzikd41 ;

<https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> {
    event:pi2jpw9a1q00d11kr9ez
            a                         msg:FromExternal ;
            msg:previousMessage    event:eczqg8lp7xbukpzikd41 , event:273p25fz6re5tp6drfsd ;
            msg:receivedTimestamp  1513170819939 ;
            msg:protocolVersion       "1.0" .
```

**diff** with all-messages-acknowledged.trig:

```
441a442,450
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> {
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
> }
547a557,580
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:49hcv8na1594ijbpx3hp ;
>             msg:sentTimestamp  1513170819628 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
> 
596a630,670
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
>     
>     event:49hcv8na1594ijbpx3hp
>             a                            msg:FromSystem ;
>             msg:correspondingRemoteMessage
>                     event:pi2jpw9a1q00d11kr9ez ;
>             msg:messageType           msg:SuccessResponse ;
>             msg:previousMessage       event:8950tg6pjze6lr52pq3y , event:rrmkri9cdrtvp1bkjcnl ;
>             msg:receivedTimestamp     1513170819409 ;
>             msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
>             msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
>             msg:recipientNode          <https://localhost:8443/won/resource> ;
>             msg:sender                conn:b4vtw60q5p3ro3yfjybs ;
>             msg:senderAtom            atom:2615528351738345500 ;
>             msg:senderNode            <https://localhost:8443/won/resource> ;
>             msg:isRemoteResponseTo       event:eczqg8lp7xbukpzikd41 ;
>             msg:isResponseTo             event:8950tg6pjze6lr52pq3y ;
>             msg:isResponseToMessageType  msg:ConnectionMessage ;
>             msg:protocolVersion          "1.0" .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq>
>             a                      msg:EnvelopeGraph ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:49hcv8na1594ijbpx3hp .
>     
>     <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMEAiz2f9ajxin8AOua0owcQBeSQVzG/AfI1+a5tIm6t3ZPtmK6MoWFAnHESOEWVUxwIwEmkyGXont/hM/s/MWOqMcEKs3nZ3XOb/3zBcjGnqKGCkJdti8vrEeHhAwlFTHIdb" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "Kp873YDlEeX3AoV1fjtNoe747auudnnAavdPJ6kLIKxUZ3Mf+3PVlS0PLF5fXS5zJUceoMbKe9G5yShz5o6L/3I1CNoTi0+R0Sh+Mxy3hlIj2ZyPYKDiQL0rOI67nUdnvqYPh4bQ0bBgBvBgaZhy8GqQkNRDtgh1jST35onShvw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay> .
> }
1237c1311
< 
---
>     
1245c1319
< 
---
>     
2194c2268
<             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
---
>             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:pi2jpw9a1q00d11kr9ez , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
4781c4855
<             msg:messageType           msg:FailureResponse ;
---
>             msg:messageType           msg:SuccessResponse ;
5033a5108,5117
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> {
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
> 
5270a5355,5384
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> , <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:8950tg6pjze6lr52pq3y .
>     
>     <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMFWL/YmG+EUiIA03zdKRC4ss53juNPHzFqKBi6kPYkbNB/Gbur0pMK2FX41a2yNk0AIwN6eCjYtnQ4ZVtgxU4uN9yYksY2rsbty2IxFwrCo+XMzlAwG0w/agvoAMhNOslzHW" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKU86NSVuVDEiz22JT2t6CtTV86SUwixnHzCP/vrP4UxrKJj7QaAcl8q1hbLnYZPOqBMcEbPHxN7Bc2pSITVjflJ6UDKDTZGD0CLYHuJVvDHSbl5XsKYwy8fUg/o4/Q3otkrFp/fOwmRWP01RWJF8lSl3b3+urH9TyR+A9m0tlCh" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e> .
>     
>     event:8950tg6pjze6lr52pq3y
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:o73qpj11bouvhv9pfmhx ;
>             msg:receivedTimestamp  1513170819096 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQC8o0Hjeh9ecoNCW5XLtiEBQbrRCF44lke8zWnSHMuH/cAsuf2M+MicjBNlMDmbdxwCMHpBKge8dazpmAv2tFv4lPPifZcrhVLYKICxLrjnZxA/M77pp5FTpivVyRVyHyLcjQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "Rr6gPmaT59AOAc6821EUWSw2b1hjh2ALv1uUXvdtmFHhrPvJmkuJyUxuSSH8OUcM9JpOZY0/r1w+85tVQLJI4VQ6xscU8V7pLnCeGx1w0def6DtjeLDW36/Y2aXDePS9Z5GR881EtpH+9tjwRaw6D5X2UVC09uYkso0dSL1pElk=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> .
> }
6264a6379,6380
>             msg:correspondingRemoteMessage
>                     event:8950tg6pjze6lr52pq3y ;
6930c7046,7083
< 
---
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:eczqg8lp7xbukpzikd41 , event:273p25fz6re5tp6drfsd ;
>             msg:receivedTimestamp  1513170819939 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig> , <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMA11jGjU5LwcuOW1pdtMWAUHBVeW8Mj6tYN1j0eRWA5MtkXVFdn//7awdr18/zt5bAIwVs6RikAKg8uct0inNm0R/Ryir3Z0yrXO+5nJdn9mLADtuXhG+Uf4saCgT3qlAPuz" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AJYrzqWsP3oknOJwdB1xDEdIeammtUVGiyhQHxEiaQIgTVM7rxDJppG98oxPevBIcaCyMPS1CgnXGP5JhgN5HnSJRKveG2oWzkHlpB+te8i5nTWGWPO1XCJe9PSmOcOEz+JJg0hpS6W8pEwHmc+zlApVoXdK0IgKbonSIEwiXFjx" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i> .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQCSdoT+ujH2W+HDFJApoyJAzYmi+ZJL+emt1Ry5l99/HHv0sI8FPwc7f4jkksTltLACMHfWEeJNwe05o3IX9/deGxQTzeALz/KUnNXr5r5RLnjvpPie0gZf3/5dhA8rfjGthg==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKvqWhwTCfU3z2/KKn+RE1SKV4MdAHGCWm3LiZphtT649dkMwM90w/uqGWHh1KSClOZgtildtzk/1rQx1r9fgWpLi/99mznV0UJfGaQOv1xvrkDHiMYgjWeOlGNFkoBsxSLuqf7mK6eg65+t/82ch1rVdp/c8I/GeggLjSB6twqe" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> .
> }
6980a7134,7142
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
> }
7306c7468
<             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf ;
---
>             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf , event:pi2jpw9a1q00d11kr9ez ;
7313c7475
<             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig> , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
7324a7487,7494
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
>     
8459a8630,8652
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> {
>     event:8950tg6pjze6lr52pq3y
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:eczqg8lp7xbukpzikd41 ;
>             msg:sentTimestamp  1513170818740 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:8950tg6pjze6lr52pq3y .
> }
8834c9027
<             msg:previousMessage       event:o73qpj11bouvhv9pfmhx ;
---
>             msg:previousMessage       event:o73qpj11bouvhv9pfmhx , event:8950tg6pjze6lr52pq3y ;
8849c9042
<             msg:containsSignature   <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> ;
8853c9046,9053
< 
---
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
>     
9577c9777,9784
< 
---
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
>     
9591c9798
<             msg:previousMessage        event:rrmkri9cdrtvp1bkjcnl ;
---
>             msg:previousMessage       event:49hcv8na1594ijbpx3hp , event:rrmkri9cdrtvp1bkjcnl ;
9606c9813
<             msg:containsSignature <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;


-----------------------------------
```
## oneLocalMessageFailsLocallyUnrealisticTest
**input**: one-local-message-fails-locally-unrealistic.trig
**expected**: one-local-message-fails-locally-unrealistic.trig
**test name**: oneLocalMessageFailsLocallyUnrealisticTest

**diff**:

***diff** with all-messages-acknowledged.trig sample*:

```
4855c4855
<             msg:messageType           msg:FailureResponse ;
---
>             msg:messageType           msg:SuccessResponse ;
10171a10172,10175
> <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
>     event:eczqg8lp7xbukpzikd41
>             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
> }
```

--------------------------
## oneLocalMessageFailsRemotelyTest
**input**: one-local-message-fails-remotely.trig
**expected**: one-local-message-fails-remotely.trig
**test name**: oneLocalMessageFailsRemotelyTest

**diff**: 
```
8786,8789d8785
< <https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
<     event:152dum7y56zn95qyernf
<             won:textMessage  "I'm not sure I understand you fully." .
< }
```

***diff** with all-messages-acknowledged.trig sample*:

```
6751c6751
<             msg:messageType           msg:FailureResponse ;
---
>             msg:messageType           msg:SuccessResponse ;
```

-------------------------
## oneLocalMessageWithoutRemoteMessageTest
**input**: one-local-message-without-remote-message.trig
**expected**: one-local-message-without-remote-message.trig
**test name**: oneLocalMessageWithoutRemoteMessageTest

**diff**:

```
8854c8854
< 
---
>    
9964,9968d9963
< }
< 
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
```

***diff** with all-messages-acknowledged.trig sample*:

```
441a442,450
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> {
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
> }
548c557,579
< 
---
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:49hcv8na1594ijbpx3hp ;
>             msg:sentTimestamp  1513170819628 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
598a630,670
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
>     
>     event:49hcv8na1594ijbpx3hp
>             a                            msg:FromSystem ;
>             msg:correspondingRemoteMessage
>                     event:pi2jpw9a1q00d11kr9ez ;
>             msg:messageType           msg:SuccessResponse ;
>             msg:previousMessage       event:8950tg6pjze6lr52pq3y , event:rrmkri9cdrtvp1bkjcnl ;
>             msg:receivedTimestamp     1513170819409 ;
>             msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
>             msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
>             msg:recipientNode          <https://localhost:8443/won/resource> ;
>             msg:sender                conn:b4vtw60q5p3ro3yfjybs ;
>             msg:senderAtom            atom:2615528351738345500 ;
>             msg:senderNode            <https://localhost:8443/won/resource> ;
>             msg:isRemoteResponseTo       event:eczqg8lp7xbukpzikd41 ;
>             msg:isResponseTo             event:8950tg6pjze6lr52pq3y ;
>             msg:isResponseToMessageType  msg:ConnectionMessage ;
>             msg:protocolVersion          "1.0" .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq>
>             a                      msg:EnvelopeGraph ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:49hcv8na1594ijbpx3hp .
>     
>     <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMEAiz2f9ajxin8AOua0owcQBeSQVzG/AfI1+a5tIm6t3ZPtmK6MoWFAnHESOEWVUxwIwEmkyGXont/hM/s/MWOqMcEKs3nZ3XOb/3zBcjGnqKGCkJdti8vrEeHhAwlFTHIdb" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "Kp873YDlEeX3AoV1fjtNoe747auudnnAavdPJ6kLIKxUZ3Mf+3PVlS0PLF5fXS5zJUceoMbKe9G5yShz5o6L/3I1CNoTi0+R0Sh+Mxy3hlIj2ZyPYKDiQL0rOI67nUdnvqYPh4bQ0bBgBvBgaZhy8GqQkNRDtgh1jST35onShvw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay> .
> }
2196c2268
<             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
---
>             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:pi2jpw9a1q00d11kr9ez , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
5035a5108,5116
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> {
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
5273a5355,5384
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> , <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:8950tg6pjze6lr52pq3y .
>     
>     <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMFWL/YmG+EUiIA03zdKRC4ss53juNPHzFqKBi6kPYkbNB/Gbur0pMK2FX41a2yNk0AIwN6eCjYtnQ4ZVtgxU4uN9yYksY2rsbty2IxFwrCo+XMzlAwG0w/agvoAMhNOslzHW" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKU86NSVuVDEiz22JT2t6CtTV86SUwixnHzCP/vrP4UxrKJj7QaAcl8q1hbLnYZPOqBMcEbPHxN7Bc2pSITVjflJ6UDKDTZGD0CLYHuJVvDHSbl5XsKYwy8fUg/o4/Q3otkrFp/fOwmRWP01RWJF8lSl3b3+urH9TyR+A9m0tlCh" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e> .
>     
>     event:8950tg6pjze6lr52pq3y
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:o73qpj11bouvhv9pfmhx ;
>             msg:receivedTimestamp  1513170819096 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQC8o0Hjeh9ecoNCW5XLtiEBQbrRCF44lke8zWnSHMuH/cAsuf2M+MicjBNlMDmbdxwCMHpBKge8dazpmAv2tFv4lPPifZcrhVLYKICxLrjnZxA/M77pp5FTpivVyRVyHyLcjQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "Rr6gPmaT59AOAc6821EUWSw2b1hjh2ALv1uUXvdtmFHhrPvJmkuJyUxuSSH8OUcM9JpOZY0/r1w+85tVQLJI4VQ6xscU8V7pLnCeGx1w0def6DtjeLDW36/Y2aXDePS9Z5GR881EtpH+9tjwRaw6D5X2UVC09uYkso0dSL1pElk=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> .
> }
6267a6379,6380
>             msg:correspondingRemoteMessage
>                     event:8950tg6pjze6lr52pq3y ;
6932a7046,7083
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:eczqg8lp7xbukpzikd41 , event:273p25fz6re5tp6drfsd ;
>             msg:receivedTimestamp  1513170819939 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig> , <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMA11jGjU5LwcuOW1pdtMWAUHBVeW8Mj6tYN1j0eRWA5MtkXVFdn//7awdr18/zt5bAIwVs6RikAKg8uct0inNm0R/Ryir3Z0yrXO+5nJdn9mLADtuXhG+Uf4saCgT3qlAPuz" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AJYrzqWsP3oknOJwdB1xDEdIeammtUVGiyhQHxEiaQIgTVM7rxDJppG98oxPevBIcaCyMPS1CgnXGP5JhgN5HnSJRKveG2oWzkHlpB+te8i5nTWGWPO1XCJe9PSmOcOEz+JJg0hpS6W8pEwHmc+zlApVoXdK0IgKbonSIEwiXFjx" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i> .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQCSdoT+ujH2W+HDFJApoyJAzYmi+ZJL+emt1Ry5l99/HHv0sI8FPwc7f4jkksTltLACMHfWEeJNwe05o3IX9/deGxQTzeALz/KUnNXr5r5RLnjvpPie0gZf3/5dhA8rfjGthg==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKvqWhwTCfU3z2/KKn+RE1SKV4MdAHGCWm3LiZphtT649dkMwM90w/uqGWHh1KSClOZgtildtzk/1rQx1r9fgWpLi/99mznV0UJfGaQOv1xvrkDHiMYgjWeOlGNFkoBsxSLuqf7mK6eg65+t/82ch1rVdp/c8I/GeggLjSB6twqe" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> .
> }
6982a7134,7143
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
> }
> 
7307c7468
<             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf ;
---
>             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf , event:pi2jpw9a1q00d11kr9ez ;
7314c7475
<             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig>  , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
7326c7487,7494
< 
---
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
>     
8461a8630,8653
> <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2> {
>     event:8950tg6pjze6lr52pq3y
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:eczqg8lp7xbukpzikd41 ;
>             msg:sentTimestamp  1513170818740 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-pkd2>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:8950tg6pjze6lr52pq3y .
> }
> 
8835c9027
<             msg:previousMessage       event:o73qpj11bouvhv9pfmhx  ;
---
>             msg:previousMessage       event:o73qpj11bouvhv9pfmhx , event:8950tg6pjze6lr52pq3y ;
8850c9042
<             msg:containsSignature   <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/o73qpj11bouvhv9pfmhx#envelope-8x4e-sig> ;
8854c9046,9053
< 
---
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
>     
9578c9777,9784
< 
---
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
>     
9592c9798
<             msg:previousMessage       event:rrmkri9cdrtvp1bkjcnl ;
---
>             msg:previousMessage       event:49hcv8na1594ijbpx3hp , event:rrmkri9cdrtvp1bkjcnl ;
9607c9813
<             msg:containsSignature  <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;

---------
```
## oneLocalMessageWithoutRemoteMessageTest
**input**: one-local-message-without-remote-message.trig
**expected**: one-local-message-without-remote-message.trig
**test name**: oneLocalMessageWithoutRemoteMessageTest

**diff**:
```
8854c8854
< 
---
>    
9964,9968d9963
< }
< 
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
```

***diff** with all-messages-acknowledged.trig sample*:
```
599,637d598
< <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> {
<     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
<             a                               sig:Signature ;
<             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
<             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
<             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
<             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
<             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
<     
<     event:49hcv8na1594ijbpx3hp
<             a                            msg:FromSystem ;
<             msg:messageType           msg:SuccessResponse ;
<             msg:previousMessage       event:8950tg6pjze6lr52pq3y , event:rrmkri9cdrtvp1bkjcnl ;
<             msg:receivedTimestamp     1513170819409 ;
<             msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
<             msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
<             msg:recipientNode          <https://localhost:8443/won/resource> ;
<             msg:sender                conn:b4vtw60q5p3ro3yfjybs ;
<             msg:senderAtom            atom:2615528351738345500 ;
<             msg:senderNode            <https://localhost:8443/won/resource> ;
<             msg:isRemoteResponseTo       event:eczqg8lp7xbukpzikd41 ;
<             msg:isResponseTo             event:8950tg6pjze6lr52pq3y ;
<             msg:isResponseToMessageType  msg:ConnectionMessage ;
<             msg:protocolVersion          "1.0" .
<     
<     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq>
<             a                      msg:EnvelopeGraph ;
<             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
<             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
<                     event:49hcv8na1594ijbpx3hp .
<     
<     <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig>
<             a                               sig:Signature ;
<             sig:signatureValue           "MGQCMEAiz2f9ajxin8AOua0owcQBeSQVzG/AfI1+a5tIm6t3ZPtmK6MoWFAnHESOEWVUxwIwEmkyGXont/hM/s/MWOqMcEKs3nZ3XOb/3zBcjGnqKGCkJdti8vrEeHhAwlFTHIdb" ;
<             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
<             msg:hash                     "Kp873YDlEeX3AoV1fjtNoe747auudnnAavdPJ6kLIKxUZ3Mf+3PVlS0PLF5fXS5zJUceoMbKe9G5yShz5o6L/3I1CNoTi0+R0Sh+Mxy3hlIj2ZyPYKDiQL0rOI67nUdnvqYPh4bQ0bBgBvBgaZhy8GqQkNRDtgh1jST35onShvw=" ;
<             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
<             msg:signedGraph              <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay> .
< }
5075,5083d5035
< <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> {
<     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
<             a                               sig:Signature ;
<             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
<             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
<             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
<             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
<             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
< }
9699,9706c9651
<     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
<             a                               sig:Signature ;
<             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
<             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
<             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
<             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
<             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
<     
---
> 
9720c9665
<             msg:previousMessage       event:49hcv8na1594ijbpx3hp , event:rrmkri9cdrtvp1bkjcnl ;
---
>             msg:previousMessage       event:rrmkri9cdrtvp1bkjcnl ;
9735c9680
<             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
10092,10096d10036
< }
< 
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .

-----------

## oneLocalMessageWithoutRemoteResponseTest
**input**: one-local-message-without-remote-response.trig 
**expected**: one-local-message-without-remote-response.trig 
**test name**: oneLocalMessageWithoutRemoteResponseTest

**diff**:

```
10039,10043d10038
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
< }
```

***diff** with all-messages-acknowledged.trig sample*:

```
441a442,450
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> {
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
> }
548c557,579
< 

> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:49hcv8na1594ijbpx3hp ;
>             msg:sentTimestamp  1513170819628 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
598a630,670
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> {
>     <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMBglgo8mkSNupDXrJ8ZAhBBp9upr3dA9+sj9vyYJMjqdEf0TJfAOwhzdwzox1ik8pwIwNrVWYrE7m+E8fEiSX0bcnt5GW0XWG9idRE/J7ULhW8PVkGdC52jV5hdoZ24/efv6" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "UjKuHV4IjR5uYuKClP0g2+8/Ox9BKZjC5aj9GHqxYEysm5W76OBq15q9mwG4uxNfnj0NYMzcyJcWL8+n761sMa5EIAhzI5U+pHWzfAbZKeJEnWGanwoCdqd1A9Taf0fvrmo/PXPIisrvFaMuTOv7H7t+lYtq2xCTerFY/GztKhw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu> .
>     
>     event:49hcv8na1594ijbpx3hp
>             a                            msg:FromSystem ;
>             msg:correspondingRemoteMessage
>                     event:pi2jpw9a1q00d11kr9ez ;
>             msg:messageType           msg:SuccessResponse ;
>             msg:previousMessage       event:8950tg6pjze6lr52pq3y , event:rrmkri9cdrtvp1bkjcnl ;
>             msg:receivedTimestamp     1513170819409 ;
>             msg:recipient              conn:lhrkbkodc0y4rmc7z51y ;
>             msg:recipientAtom          atom:ekdwt2a60tesl77r13mu ;
>             msg:recipientNode          <https://localhost:8443/won/resource> ;
>             msg:sender                conn:b4vtw60q5p3ro3yfjybs ;
>             msg:senderAtom            atom:2615528351738345500 ;
>             msg:senderNode            <https://localhost:8443/won/resource> ;
>             msg:isRemoteResponseTo       event:eczqg8lp7xbukpzikd41 ;
>             msg:isResponseTo             event:8950tg6pjze6lr52pq3y ;
>             msg:isResponseToMessageType  msg:ConnectionMessage ;
>             msg:protocolVersion          "1.0" .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq>
>             a                      msg:EnvelopeGraph ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8950tg6pjze6lr52pq3y#envelope-7hiu-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:49hcv8na1594ijbpx3hp .
>     
>     <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMEAiz2f9ajxin8AOua0owcQBeSQVzG/AfI1+a5tIm6t3ZPtmK6MoWFAnHESOEWVUxwIwEmkyGXont/hM/s/MWOqMcEKs3nZ3XOb/3zBcjGnqKGCkJdti8vrEeHhAwlFTHIdb" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "Kp873YDlEeX3AoV1fjtNoe747auudnnAavdPJ6kLIKxUZ3Mf+3PVlS0PLF5fXS5zJUceoMbKe9G5yShz5o6L/3I1CNoTi0+R0Sh+Mxy3hlIj2ZyPYKDiQL0rOI67nUdnvqYPh4bQ0bBgBvBgaZhy8GqQkNRDtgh1jST35onShvw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay> .
> }
2196c2268
<             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
---
>             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:pi2jpw9a1q00d11kr9ez , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
5035a5108,5116
> <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> {
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
6964a7046,7083
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:eczqg8lp7xbukpzikd41 , event:273p25fz6re5tp6drfsd ;
>             msg:receivedTimestamp  1513170819939 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig> , <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMA11jGjU5LwcuOW1pdtMWAUHBVeW8Mj6tYN1j0eRWA5MtkXVFdn//7awdr18/zt5bAIwVs6RikAKg8uct0inNm0R/Ryir3Z0yrXO+5nJdn9mLADtuXhG+Uf4saCgT3qlAPuz" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AJYrzqWsP3oknOJwdB1xDEdIeammtUVGiyhQHxEiaQIgTVM7rxDJppG98oxPevBIcaCyMPS1CgnXGP5JhgN5HnSJRKveG2oWzkHlpB+te8i5nTWGWPO1XCJe9PSmOcOEz+JJg0hpS6W8pEwHmc+zlApVoXdK0IgKbonSIEwiXFjx" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i> .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQCSdoT+ujH2W+HDFJApoyJAzYmi+ZJL+emt1Ry5l99/HHv0sI8FPwc7f4jkksTltLACMHfWEeJNwe05o3IX9/deGxQTzeALz/KUnNXr5r5RLnjvpPie0gZf3/5dhA8rfjGthg==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKvqWhwTCfU3z2/KKn+RE1SKV4MdAHGCWm3LiZphtT649dkMwM90w/uqGWHh1KSClOZgtildtzk/1rQx1r9fgWpLi/99mznV0UJfGaQOv1xvrkDHiMYgjWeOlGNFkoBsxSLuqf7mK6eg65+t/82ch1rVdp/c8I/GeggLjSB6twqe" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> .
> }
7349c7468
<             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf ;
---
>             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf , event:pi2jpw9a1q00d11kr9ez ;
7356c7475
<             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig>  , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
7368c7487,7494
< 
---
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
>     
9651c9777,9784
< 
---
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
>     
9665c9798
<             msg:previousMessage       event:rrmkri9cdrtvp1bkjcnl ;
---
>             msg:previousMessage       event:49hcv8na1594ijbpx3hp , event:rrmkri9cdrtvp1bkjcnl ;
9680c9813
<             msg:containsSignature  <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;
---
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> , <https://localhost:8443/won/resource/event/rrmkri9cdrtvp1bkjcnl#envelope-uqay-sig> ;

< 
```

--------------
## oneLocalMessageWithoutRemoteRemoteResponseTest
**input**: one-local-message-without-remote-remote-response.trig
**expected**: one-local-message-without-remote-remote-response.trig
**test name**: oneLocalMessageWithoutRemoteRemoteResponseTest

**diff**:

```
10094,10098d10093
< <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#content-xg7t> {
<     event:eczqg8lp7xbukpzikd41
<             won:textMessage  "    'deactivate':  deactivate remote atom of the current connection" .
< }
< 
```

***diff** with all-messages-acknowledged.trig sample*:

```
441a442,450
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> {
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
> }
548c557,579
< 

> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                     msg:FromExternal ;
>             msg:correspondingRemoteMessage
>                     event:49hcv8na1594ijbpx3hp ;
>             msg:sentTimestamp  1513170819628 ;
>             msg:protocolVersion   "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMAlEwKD5FAMaKyUwm7W6LHLLRdEG7SLizy+ev40Dmkux8pqjs88S1qc0eHJ30c0k4QIwX27IDuGaxk21COf+81YXznH55gV5AhF3xQ9VaN0/yP3hdYphke9/8mCzOIK/KsRr" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "SZFt+I+SbyhZTAv92sY3LxoA3cA3yxs4AKvLtzmwDx1tj1r/Xx+AjD9bkjQRc3Wb7sA+YLQF6MpEHdqRDlCcR1pX+7nVYs6yvdPq2RGV7pbZIwlN9aCg7/AsGS7nFXWqhxURl4v2ecX2N663aO3aIgFTQBLLu7xEKugbbpsRQQw=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/49hcv8na1594ijbpx3hp#envelope-swnq> .
> }
609a641,642
>             msg:correspondingRemoteMessage
>                     event:pi2jpw9a1q00d11kr9ez ;
2235c2268
<             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .

>             rdfs:member  event:kaj9nimgw0lkcgmb1asf , event:h6c8epmrvb3gxqrvs81d , event:f3u4lc7l1czvps18lemf , event:ivr0xermk4aeb3yhohk4 , event:v5rvsrlg0x3ogdqyfuy4 , event:u02ulpncdj9l8ae8x2aq , event:cbcccoqqqbec6bxkl3y3 , event:i1frxy9ikewwjuyjcznt , event:59gtirhola4ydvddyo0k , event:xbhopokox5b4vz78e59w , event:tlyivx8nn93zw41ujn1o , event:csvglzqkcoddoreep5w0 , event:alif190jj9we7toczas8 , event:mmdq8395owv3xy9iyl5q , event:lur3g5en41crth556538 , event:i3k0giied2bgp0p44h7u , event:253k6pqq8gyttmmfxc7l , event:vj2yzrtlf700wixuxujw , event:t87usy00t0o9h4f0d1p3 , event:s9a226k3n70ihhaurhdp , event:4e2ws6ap0hp93iv0oyu2 , event:6fvg967tlh9rho8axvbs , event:xqsfn58cdatb7ryp8lzt , event:8c6o81ry6mxeetm2d2pf , event:ofx1afjv35cwpppp0wyg , event:cw64ogmt2lv6n4kdjdxh , event:v66scoiidw56iam8q86p , event:2uf9sj4itmz9fpas7suo , event:wlv9kjrh93gfzetdojp4 , event:to3x48329wwbanylmar0 , event:pj2n6r9g6jt39rv99xh0 , event:mv0xoe06cxsxt08s7guf , event:6m7oi7hxwvoi2pmguo6d , event:2sbarcz1yu7cenpcghay , event:xu87g40eu8cx053lad08 , event:x7cjarywf0513459swe0 , event:iasuj1z9fva0svkfqb79 , event:8gvplnjrinoqay8ycrc2 , event:9uyongxoip0kz7t9tsa8 , event:1tr3o22co1907d6b6n7s , event:mthg8rh8r4grme5ph2eh , event:5s66o8cqv4rxv74xfepg , event:orj8iruy8pcer6zzxlra , event:zvx39rdagwj6o7nfiw1r , event:m8b6jvgclclzy48p7wqd , event:4y6xcnpgc0xk4relqfox , event:t6d7eq3cq6nq54a16k1w , event:izq6icbkftfbzm0clxeu , event:cgqt5h004iql2003me2n , event:152dum7y56zn95qyernf , event:9oa8ktxu7tqzll06rhw9 , event:rig33yoxaetjw059bzuw , event:0uouhqi6aym8jad508kd , event:ck5071fsyaned6upryxj , event:xsbbah2dhkcg6d3h13gk , event:s9zfgm2iika5vgvayt7h , event:8h7v5ml1aflqmoyem61a , event:0s55ww5ae82lf3j3gwaq , event:n5rqfwjqbcpdwqjjwpdw , event:eczqg8lp7xbukpzikd41 , event:5r6qvd7rennbi148vunk , event:fn87pwzr9g4a9v368h4m , event:csridlusp0h45v9gf9v6 , event:xpspyx3bpyev5v5p1vf6 , event:pi2jpw9a1q00d11kr9ez , event:joo1uifc1fc2k6fk5z8t , event:plmfevq4c12f8itbisjf , event:xp44teiwtooczc14npe2 , event:4xjx598ewu7579zpl64p , event:bxwevm9gzqconmzxji4u , event:gv6zk2yqk6o8bl574n36 , event:ggbuodgi1pykilp8znve , event:collk6egdkt2tey39h8z , event:93e94yjsmx9l4lg8lu1b , event:eia6yrvml8v995ueq65m , event:cvloufr7mmg0siwflisg , event:xkeovy4cf48spd5euwj1 , event:4ongmje7w2v03mp7ztex , event:m4nq3rl0m1br8bea2n72 , event:eue8ar55z7as596cu33m , event:273p25fz6re5tp6drfsd , event:66nnn87elpe5995a5wjv , event:uu3ciy3btq6tg90crr3b , event:d3jq9fgiu47gdhclj7h0 , event:ih9v6gyllshhvo5kxyv0 , event:sdt7yr7q7t9iw8wcuu3m , event:dhdnzy40wlrnxh7ymr2b , event:cqvzpvoqvtkylzdybhej .
7012a7046,7083
> <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> {
>     event:pi2jpw9a1q00d11kr9ez
>             a                         msg:FromExternal ;
>             msg:previousMessage    event:eczqg8lp7xbukpzikd41 , event:273p25fz6re5tp6drfsd ;
>             msg:receivedTimestamp  1513170819939 ;
>             msg:protocolVersion       "1.0" .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa>
>             a                      msg:EnvelopeGraph ;
>             msg:containsEnvelope   <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> ;
>             msg:containsSignature  <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig> , <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig> ;
>             <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
>                     event:pi2jpw9a1q00d11kr9ez .
>     
>     <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCMA11jGjU5LwcuOW1pdtMWAUHBVeW8Mj6tYN1j0eRWA5MtkXVFdn//7awdr18/zt5bAIwVs6RikAKg8uct0inNm0R/Ryir3Z0yrXO+5nJdn9mLADtuXhG+Uf4saCgT3qlAPuz" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AJYrzqWsP3oknOJwdB1xDEdIeammtUVGiyhQHxEiaQIgTVM7rxDJppG98oxPevBIcaCyMPS1CgnXGP5JhgN5HnSJRKveG2oWzkHlpB+te8i5nTWGWPO1XCJe9PSmOcOEz+JJg0hpS6W8pEwHmc+zlApVoXdK0IgKbonSIEwiXFjx" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/273p25fz6re5tp6drfsd#envelope-vg7i> .
>     
>     <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGQCME+3NyuHMjfzR8ibEAysUg8pQyRYqVuoJ15oohfkDr8iinjDGdUNGAWXXSCVEvUf/wIwWrF8CKtjb++4Dj4sJ9/ea7X4iqzs0MxECJW+fMaBcprLJ+nk3BU5yTn3v7bfQxht" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "eEiYLRNLiVOCjTzytc6t8LPhsMA1Ia80xObgp2b3ZonLKiD9F5LfwSccvzS0HEBTGxAy2eoZj1hh9/XLGzE92ZxmdhGcc1BxyyDWA3aAEzOEsPKgrW4U7UhvK9D3R7lJGxaLC8LKIlD5LyWqVpld9MO5pAXDO8EO+2sDBMNN0pY=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/eczqg8lp7xbukpzikd41#envelope-cd9k> .
>     
>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMQCSdoT+ujH2W+HDFJApoyJAzYmi+ZJL+emt1Ry5l99/HHv0sI8FPwc7f4jkksTltLACMHfWEeJNwe05o3IX9/deGxQTzeALz/KUnNXr5r5RLnjvpPie0gZf3/5dhA8rfjGthg==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "AKvqWhwTCfU3z2/KKn+RE1SKV4MdAHGCWm3LiZphtT649dkMwM90w/uqGWHh1KSClOZgtildtzk/1rQx1r9fgWpLi/99mznV0UJfGaQOv1xvrkDHiMYgjWeOlGNFkoBsxSLuqf7mK6eg65+t/82ch1rVdp/c8I/GeggLjSB6twqe" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-xvip> .
> }
7397c7468
<             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf ;

>             msg:previousMessage    event:8c6o81ry6mxeetm2d2pf , event:pi2jpw9a1q00d11kr9ez ;
7404c7475
<             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig>  , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;

>             msg:containsSignature  <https://localhost:8443/won/resource/event/8c6o81ry6mxeetm2d2pf#envelope-g9px-sig> , <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig> , <https://localhost:8443/won/resource/event/eue8ar55z7as596cu33m#envelope-h4dx-sig> ;
7416c7487,7494
< 

>     <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa-sig>
>             a                               sig:Signature ;
>             sig:signatureValue           "MGUCMCxiytXBuh5w3NE7kdsAUsVjzJGK+tF2KgU5+AEyO3DTzGigsBoen0nQUM1l/n09XQIxAOnJ3IidktXW2rZHoBql+fazmmhrqXnDffE9Us5ux/rL+izk9q94Z/T8BcUHiKSzmQ==" ;
>             sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
>             msg:hash                     "bm/4QvtUDDVrZLPsj72ecL/AEWekrp27ZkOyAwdYk8gvcmIXDSgFqqiLc1HuR8NQSEM1ZNP8xPFKSC2uWiOFiscTul+6HVBmISweVHKVWabTBFcp+7uMkhxBRM1E+djcfk8KJ3RAKYCQcNF2lpa+rrtKGR9uKwe/gmDFxZqe9gg=" ;
>             msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
>             msg:signedGraph              <https://localhost:8443/won/resource/event/pi2jpw9a1q00d11kr9ez#envelope-flqa> .
>  
```







