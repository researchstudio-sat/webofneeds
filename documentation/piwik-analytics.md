
This is a ...


Event documentation:
https://piwik.org/docs/event-tracking/

An example JavaScript Tracking Code:

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


Some examples:
<a href="http://localhost/test1/" onclick="javascript:_paq.push(['trackEvent', 'Menu', 'Freedom']);">Freedom page</a>

<input type="submit" onclick="_paq.push(['trackEvent', 'Category', 'Name', 'Description', 8.18]); " value="Click" name="commit">










