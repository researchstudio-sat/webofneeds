
This documents provides the basic information on our Piwik/WoN implementation. 

In general, we use event tracking feature of Piwik for our analytics. 

You can find the event documentation of Piwik here:
https://piwik.org/docs/event-tracking/

As it is described in the documentation of Piwik a JavaScript Tracking Code has to be added to every page that uses Piwik. Since Won consists of only one page, this has only used once.

An example JavaScript Tracking Code:
```HTML
<!-- Piwik -->
<script type="text/javascript">
  var _paq = _paq || [];
  /* tracker methods like "setCustomDimension" should be called before "trackPageView" */
  _paq.push(['trackPageView']);
  _paq.push(['enableLinkTracking']);
  (function() {
    var u="//localhost/piwik/";
    _paq.push(['setTrackerUrl', u+'piwik.php']);
    _paq.push(['setSiteId', '1']);
    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
    g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'piwik.js'; s.parentNode.insertBefore(g,s);
  })();
</script>
<!-- End Piwik Code -->
```

Here are some examples of event tracking by Piwik:

```HTML
<a href="http://localhost/test1/" onclick="javascript:_paq.push(['trackEvent', 'Menu', 'Freedom']);">Freedom page</a>

<input type="submit" onclick="_paq.push(['trackEvent', 'Category', 'Name', 'Description', 8.18]); " value="Click" name="commit">
```









