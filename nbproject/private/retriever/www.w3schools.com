
<!DOCTYPE html>
<html lang="en-US">
<head>
<title>W3Schools Online Web Tutorials</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="Keywords" content="HTML,CSS,JavaScript,DOM,jQuery,PHP,SQL,XML,Bootstrap,Web,W3CSS,W3C,tutorials,programming,development,training,learning,quiz,primer,lessons,reference,examples,source code,colors,demos,tips,w3c">
<link rel="icon" href="/favicon.ico" type="image/x-icon">
<link rel="stylesheet" href="/lib/w3.css">
<script>
(function() {
var cx = '012971019331610648934:m2tou3_miwy';
var gcse = document.createElement('script'); gcse.type = 'text/javascript'; gcse.async = true;
gcse.src = 'https://www.google.com/cse/cse.js?cx=' + cx;
var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(gcse, s);
})();
(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');
ga('create', 'UA-3855518-1', 'auto');
ga('require', 'displayfeatures');
ga('send', 'pageview');
</script>
<style>
/* W3Schools Font Logo */
.w3schools-logo {
font-family:fontawesome;
text-decoration:none;
line-height: 1;
-webkit-font-smoothing: antialiased;
-moz-osx-font-smoothing: grayscale;
font-size:37px;
letter-spacing:3px;
color:#555555;
display:block;
position:relative;
}
.w3schools-logo .dotcom {color:#4CAF50;}
@font-face {
font-family:'fontawesome';
src:url('../lib/fonts/fontawesome.eot?14663396#iefix') format('embedded-opentype'),
url('../lib/fonts/fontawesome.woff?14663396') format('woff'),
url('../lib/fonts/fontawesome.ttf?14663396') format('truetype'),
url('../lib/fonts/fontawesome.svg?14663396#fontawesome') format('svg');
font-style:normal;
}
.fa {
display:inline-block;
font:normal normal normal 14px/1 FontAwesome;
font-size:20px;
text-rendering:auto;
-webkit-font-smoothing:antialiased;
-moz-osx-font-smoothing:grayscale;
transform:translate(0,0);
}
.fa-home:before {content:'\e800';}
.fa-globe:before {content:'\e801';}
.fa-search:before {content:'\e802'; }
.fa-thumbs-o-up:before {content:'\e803';}
.fa-left-open:before {content:'\e804';}
.fa-right-open:before {content:'\e805';}
.fa-caret-down:before {content:'\e809';}
.fa-caret-up:before {content:'\e80a';}
/* Google */
#nav_translate, #nav_search {display:none;}
#nav_translate a {display:inline;}
#googleSearch {color:#000000;}
.searchdiv {
max-width:400px;
margin:auto;
text-align:left;
font-size:16px;
}
div.cse .gsc-control-cse, div.gsc-control-cse {
background-color:transparent;
border:none;
padding:0px;
margin:0px;
}
td.gsc-search-button input.gsc-search-button {background-color:#555555;border-color:#555555;}
input.gsc-input, .gsc-input-box, .gsc-input-box-hover, .gsc-input-box-focus, .gsc-search-button {
box-sizing:content-box;
line-height:normal;
}
.gsc-tabsArea div {overflow:visible;}
.gsst_a .gscb_a {margin-top:3px;}
/* Customize W3.CSS */
.w3-dropnav {
display:none;
padding-bottom:40px;
position:absolute;
width:100%;
z-index:99 !important;
}
.w3-col .w3-btn {
margin:5px 5px 5px 0;
font-size:16px;
}
.w3-col.l4 .w3-card-2 {
padding:15px 10px;
height:260px;
}
.w3-closebtn {
padding:10px 20px !important;
position:absolute;
right:0;
}
.w3-closebtn:hover {background-color:#cccccc;}
.w3-theme {color:#fff !important;background-color:#4CAF50 !important;}
.w3-accordion-content .w3-closebtn{display:none;}
.w3-main {margin-left:230px;}
.w3-sidenav {
z-index:3;
width:230px;
overflow:hidden !important;
position:absolute !important;
margin-bottom:-155px;
}
.w3-sidenav a {
padding:0 16px;
font-size:16px;
}
.w3-navbar {
position:relative;
z-index:4;
font-size:17px;
}
h1 {
font-size:80px;
color:#555555;
margin:2px 0 -20px 0 !important;
}
@media screen and (min-width:769px){.w3-sidenav.w3-collapse{display:block !important;}}
@media screen and (min-width:769px){.w3-opennav,.w3-accordion{display:none !important;}}
@media screen and (min-width:769px){.w3-main{margin-left:230px !important;}}
@media screen and (max-width:768px){.w3-sidenav.w3-collapse,.navbarbtns,.navex{display:none !important;}h1,.w3-jumbo{font-size:60px !important;}.w3-padding-jumbo{padding:32px !important;}}
</style>
<!--[if lt IE 9]>
<script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
<script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
<![endif]-->
</head>
<body style="position:relative;min-height:100%;">

<div class="w3-row w3-white w3-padding w3-hide-medium w3-hide-small" style="margin-top:5px;">
 <div class="w3-half" style="margin:4px 0 6px 0"><a class='w3schools-logo' href='//www.w3schools.com'>w3schools<span class='dotcom'>.com</span></a></div>
 <div class="w3-half w3-margin-top w3-wide w3-hide-medium w3-hide-small">
  <div class="w3-right" style="position:relative;top:5px;">THE WORLD'S LARGEST WEB DEVELOPER SITE</div>
 </div>
</div>

<div class="w3-hide-large w3-hide-large w3-padding-12" style="margin-top:8px;">
 <div class="w3-center"><a class='w3schools-logo' href='//www.w3schools.com'>w3schools<span class='dotcom'>.com</span></a></div>
 <div class="w3-center w3-wide w3-hide-small" style="margin:14px 0 -5px 0">THE WORLD'S LARGEST WEB DEVELOPER SITE</div>
</div>

<div style='display:none;position:absolute;z-index:6;right:60px;height:57px;padding-top:12px;padding-right:20px;background-color:#4CAF50;' id='googleSearch'><div class='gcse-search'></div></div>
<div style='display:none;position:absolute;z-index:5;right:120px;height:57px;background-color:#4CAF50;text-align:right;padding-top:15px;' id='google_translate_element'></div>

<ul class="w3-navbar w3-theme w3-card-2 w3-wide">
 <li class="w3-opennav">
  <a class="w3-hover-white w3-theme w3-padding-16" href="javascript:void(0)" onclick="w3_open()">&#9776;</a>
 </li>
 <li class="navbarbtns"><a class="w3-hover-white w3-padding-16" href="javascript:void(0)" onclick="w3_open_nav('tutorials')" id="navbtn_tutorials">TUTORIALS <i class='fa fa-caret-down'></i><i class='fa fa-caret-up' style="display:none"></i></a></li>
 <li class="navbarbtns"><a class="w3-hover-white w3-padding-16" href="javascript:void(0)" onclick="w3_open_nav('references')" id="navbtn_references">REFERENCES <i class='fa fa-caret-down'></i><i class='fa fa-caret-up' style="display:none"></i></a></li>
 <li class="navbarbtns"><a class="w3-hover-white w3-padding-16" href="javascript:void(0)" onclick="w3_open_nav('examples')" id="navbtn_examples">EXAMPLES <i class='fa fa-caret-down'></i><i class='fa fa-caret-up' style="display:none"></i></a></li>
 <li class="w3-right"><a class="w3-hover-white w3-padding-16 w3-right" href="javascript:void(0)" onclick="open_search(this)" title='Search W3Schools'><i class='fa'>&#xe802;</i></a></li>
 <li class="w3-right"><a class="w3-hover-white w3-padding-16 w3-right" href="javascript:void(0)" onclick="open_translate(this)" title='Translate W3Schools'><i class='fa'>&#xe801;</i></a></li>
</ul>

<nav class="w3-sidenav w3-collapse w3-white w3-card-2" id="mySidenav">
 <div class="w3-container w3-padding-top">
  <h4>HTML and CSS</h4>
 </div>
 <a href="/html/default.asp">Learn HTML</a>
 <a href="/css/default.asp">Learn CSS</a>
 <a href="/w3css/default.asp">Learn W3.CSS</a>
 <a href="/colors/default.asp">Learn Colors</a>
 <a href="/bootstrap/default.asp">Learn Bootstrap</a>
 <a href="/icons/default.asp">Learn Icons</a>
 <a href="/graphics/default.asp">Learn Graphics</a>
 <a href="/howto/default.asp">Learn How To</a>
 <div class="w3-container w3-padding-top">
  <h4>JavaScript</h4>
 </div>
 <a href="/js/default.asp">Learn JavaScript</a>
 <a href="/jquery/default.asp">Learn jQuery</a>
 <a href="/jquerymobile/default.asp">Learn jQueryMobile</a>
 <a href="/appml/default.asp">Learn AppML</a>
 <a href="/angular/default.asp">Learn AngularJS</a>
 <a href="/js/js_json_intro.asp">Learn JSON</a>
 <a href="/ajax/default.asp">Learn AJAX</a>
 <div class="w3-container w3-padding-top">
  <h4>Server Side</h4>
 </div>
 <a href="/sql/default.asp">Learn SQL</a>
 <a href="/php/default.asp">Learn PHP</a>
 <a href='/asp/default.asp'>Learn ASP</a>
 <div class="w3-container w3-padding-top">
  <h4>Web Building</h4>
 </div>
 <a href="/w3css/w3css_templates.asp">Web Templates</a>
 <a href="/browsers/default.asp">Web Statistics</a>
 <a href="/cert/default.asp">Web Certificates</a>
 <div class="w3-container w3-padding-top">
  <h4>XML Tutorials</h4>
 </div>
 <a href="/xml/default.asp">Learn XML</a>
 <a href="/xsl/default.asp">Learn XSLT</a>
 <a href='/xsl/xpath_intro.asp'>Learn XPath</a>
 <a href='/xsl/xquery_intro.asp'>Learn XQuery</a>
</nav>

<div id='myAccordion' class="w3-card-2 w3-light-grey w3-center w3-hide-large w3-accordion" style="display:none;cursor:default">
 <a href="javascript:void(0)" onclick="w3_close()" class="w3-closebtn w3-xlarge w3-margin-0">&times;</a><br>
 <div class="w3-container w3-padding-32">
  <a class="w3-btn-block w3-light-grey w3-large w3-wide w3-hover-none w3-hover-opacity" onclick="open_xs_menu('tutorials');" href="javascript:void(0);">TUTORIALS <i class='fa fa-caret-down'></i></a>
  <div id="sectionxs_tutorials" class="w3-white w3-accordion-content w3-show"></div>
  <a class="w3-btn-block w3-light-grey w3-large w3-wide w3-hover-none w3-hover-opacity" onclick="open_xs_menu('references')" href="javascript:void(0);">REFERENCES <i class='fa fa-caret-down'></i></a>
  <div id="sectionxs_references" class="w3-white w3-accordion-content w3-show"></div>
  <a class="w3-btn-block w3-light-grey w3-large w3-wide w3-hover-none w3-hover-opacity" onclick="open_xs_menu('examples')" href="javascript:void(0);">EXAMPLES <i class='fa fa-caret-down'></i></a>
  <div id="sectionxs_examples" class="w3-white w3-accordion-content w3-show"></div>
 </div>
</div>

<nav id="nav_tutorials" class="w3-dropnav w3-light-grey w3-card-2 w3-hide-small navex">
 <span onclick="w3_close_nav('tutorials')" class="w3-closebtn w3-xlarge">&times;</span><br>
 <div class="w3-row-padding w3-padding-bottom">
  <div class="w3-col l3 m6">
   <h3>HTML and CSS</h3>
   <a href="/html/default.asp">Learn HTML</a>
   <a href="/css/default.asp">Learn CSS</a>
   <a href="/w3css/default.asp">Learn W3.CSS</a>
   <a href="/colors/default.asp">Learn Colors</a>
   <a href="/bootstrap/default.asp">Learn Bootstrap</a>
   <a href="/icons/default.asp">Learn Icons</a>
   <a href="/graphics/default.asp">Learn Graphics</a>
   <a href="/howto/default.asp">Learn How To</a>
  </div>
  <div class="w3-col l3 m6">
   <h3>JavaScript</h3>
   <a href="/js/default.asp">Learn JavaScript</a>
   <a href="/jquery/default.asp">Learn jQuery</a>
   <a href="/jquerymobile/default.asp">Learn jQueryMobile</a>
   <a href="/appml/default.asp">Learn AppML</a>
   <a href="/angular/default.asp">Learn AngularJS</a>
   <a href="/js/js_json_intro.asp">Learn JSON</a>
   <a href="/ajax/default.asp">Learn AJAX</a>
   <div class="w3-hide-small"><br><br></div>
  </div>
  <div class="w3-col l3 m6">
   <h3>Server Side</h3>
   <a href="/sql/default.asp">Learn SQL</a>
   <a href="/php/default.asp">Learn PHP</a>
   <a href='/asp/default.asp'>Learn ASP</a>
   <h3>Web Building</h3>
   <a href="/w3css/w3css_templates.asp">Web Templates</a>
   <a href="/browsers/default.asp">Web Statistics</a>
   <a href="/cert/default.asp">Web Certificates</a>
  </div>
  <div class="w3-col l3 m6">
   <h3>XML Tutorials</h3>
   <a href="/xml/default.asp">Learn XML</a>
   <a href="/xsl/default.asp">Learn XSLT</a>
   <a href='/xsl/xpath_intro.asp'>Learn XPath</a>
   <a href='/xsl/xquery_intro.asp'>Learn XQuery</a>
  </div>
 </div>
</nav>

<nav id="nav_references" class="w3-dropnav w3-light-grey w3-card-2 w3-hide-small navex">
 <span onclick="w3_close_nav('references')" class="w3-closebtn w3-xlarge">&times;</span><br>
 <div class="w3-row-padding w3-padding-bottom">
  <div class="w3-col m4">
   <h3>HTML</h3>
   <a href='/tags/default.asp'>HTML Tag Reference</a>
   <a href='/tags/ref_eventattributes.asp'>HTML Event Reference</a>
   <a href='/colors/default.asp'>HTML Color Reference</a>
   <a href='/tags/ref_attributes.asp'>HTML Attribute Reference</a>
   <a href='/tags/ref_canvas.asp'>HTML Canvas Reference</a>
   <a href='/graphics/svg_reference.asp'>HTML SVG Reference</a>
   <a href='/graphics/google_maps_reference.asp'>Google Maps Reference</a>
   <h3>CSS</h3>
   <a href='/cssref/default.asp'>CSS Reference</a>
   <a href='/cssref/css_selectors.asp'>CSS Selector Reference</a>
   <a href='/w3css/w3css_references.asp'>W3.CSS Reference</a>
   <a href='/bootstrap/bootstrap_ref_css_text.asp'>Bootstrap Reference</a>
   <a href='/icons/icons_reference.asp'>Icon Reference</a>
  </div>
  <div class="w3-col m4">
   <h3>JavaScript</h3>
   <a href='/jsref/default.asp'>JavaScript Reference</a>
   <a href='/jsref/default.asp'>HTML DOM Reference</a>
   <a href='/jquery/jquery_ref_selectors.asp'>jQuery Reference</a>
   <a href='/jquerymobile/jquerymobile_ref_data.asp'>jQuery Mobile Reference</a>
   <a href='/angular/angular_ref_directives.asp'>AngularJS Reference</a>
   <h3>XML</h3>
   <a href='/xml/dom_nodetype.asp'>XML DOM Reference</a>
   <a href='/xsl/xsl_w3celementref.asp'>XSLT Reference</a>
   <a href='/xml/schema_elements_ref.asp'>XML Schema Reference</a>

  </div>
  <div class="w3-col m4">
   <h3>Character Sets</h3>
   <a href='/charsets/default.asp'>HTML Character Sets</a>
   <a href='/charsets/ref_html_ascii.asp'>HTML ASCII</a>
   <a href='/charsets/ref_html_ansi.asp'>HTML ANSI</a>
   <a href='/charsets/ref_html_ansi.asp'>HTML Windows-1252</a>
   <a href='/charsets/ref_html_8859.asp'>HTML ISO-8859-1</a>
   <a href='/charsets/ref_html_symbols.asp'>HTML Symbols</a>
   <a href='/charsets/ref_html_utf8.asp'>HTML UTF-8</a>
   <h3>Server Side</h3>
   <a href='/php/php_ref_array.asp'>PHP Reference</a>
   <a href='/sql/sql_quickref.asp'>SQL Reference</a>
   <a href='/asp/asp_ref_response.asp'>ASP Reference</a>
  </div>
 </div>
</nav>

<nav id="nav_examples" class="w3-dropnav w3-light-grey w3-card-2 w3-hide-small navex">
 <span onclick="w3_close_nav('examples')" class="w3-closebtn w3-xlarge">&times;</span><br>
 <div class="w3-row-padding w3-padding-bottom">
  <div class="w3-col l3 m6">
   <h3>HTML and CSS</h3>
   <a href='/html/html_examples.asp'>HTML Examples</a>
   <a href='/css/css_examples.asp'>CSS Examples</a>
   <a href='/w3css/w3css_examples.asp'>W3.CSS Examples</a>
   <a href='/bootstrap/bootstrap_examples.asp'>Bootstrap Examples</a>
  </div>
  <div class="w3-col l3 m6">
   <h3>JavaScript</h3>
   <a href="/js/js_examples.asp" target="_top">JavaScript Examples</a>
   <a href="/js/js_dom_examples.asp" target="_top">HTML DOM Examples</a>
   <a href="/jquery/jquery_examples.asp" target="_top">jQuery Examples</a>
   <a href="/jquerymobile/jquerymobile_examples.asp" target="_top">jQuery Mobile Examples</a>
   <a href="/angular/angular_examples.asp" target="_top">AngularJS Examples</a>
   <a href="/ajax/ajax_examples.asp" target="_top">AJAX Examples</a>
  </div>
  <div class="w3-col l3 m6">
   <h3>Server Side</h3>
   <a href="/php/php_examples.asp" target="_top">PHP Examples</a>
   <a href="/asp/asp_examples.asp" target="_top">ASP Examples</a>
  </div>
  <div class="w3-col l3 m6">
   <h3>XML</h3>
   <a href="/xml/xml_examples.asp" target="_top">XML Examples</a>
   <a href="/xsl/xsl_examples.asp" target="_top">XSLT Examples</a>
   <a href="/xsl/xpath_examples.asp" target="_top">XPath Examples</a>
   <a href="/xml/schema_example.asp" target="_top">XML Schema Examples</a>
   <a href="/graphics/svg_examples.asp" target="_top">SVG Examples</a>
  </div>
 </div>
</nav>

<!-- MAIN -->
<div class="w3-main">

<div class="w3-row w3-margin-bottom">
 <div class="w3-col l6 w3-center" style="padding:3%">
  <h1>HTML</h1>
  <p class="w3-xlarge w3-text-dark-grey">The language for building web pages</p>
  <a href="/html/default.asp" class="w3-btn w3-dark-grey">LEARN HTML</a>
  <a href="/tags/default.asp" class="w3-btn w3-dark-grey">HTML REFERENCE</a>
 </div>
 <div class="w3-col l6" style="padding:3%">
  <div class="w3-example w3-padding-16 w3-margin-0 w3-hide-small">
   <h4>HTML Example:</h4>
   <div class="w3-code htmlHigh notranslate w3-border-green">
&lt;!DOCTYPE html&gt;<br>
&lt;html&gt;<br>
&lt;title&gt;HTML Tutorial&lt;/title&gt;<br>
&lt;body&gt;<br><br>
&lt;h1&gt;This is a heading&lt;/h1&gt;<br>
&lt;p&gt;This is a paragraph.&lt;/p&gt;<br><br>
&lt;/body&gt;<br>
&lt;/html&gt;
   </div>
   <a href="/html/tryit.asp?filename=tryhtml_default" target="_blank" class="w3-btn w3-theme w3-margin-0">Try it Yourself &raquo;</a>
  </div>
 </div>
</div>

<div class="w3-row w3-light-grey w3-hide-medium w3-hide-small">
 <div class="w3-col l6" style="padding-top:40px;padding:3%;">
  <h4>CSS Example:</h4>
  <div class="w3-code cssHigh notranslate w3-card-2 w3-border-green">
body {<br>
&nbsp;&nbsp;&nbsp; background-color: lightblue;<br>}<br>
h1 {<br>
&nbsp;&nbsp;&nbsp; color: white;<br>
&nbsp;&nbsp;&nbsp; text-align: center;<br>}<br>
p {<br>
&nbsp;&nbsp;&nbsp; font-family: verdana;<br>
&nbsp;&nbsp;&nbsp; font-size: 20px;<br>}
  </div>
  <a href="/css/tryit.asp?filename=trycss_default" target="_blank" class="w3-btn w3-theme w3-margin-0">Try it Yourself &raquo;</a>
 </div>
 <div class="w3-col l6 w3-center" style="padding:3%">
  <h1>CSS</h1>
  <p class="w3-xlarge">The language for styling web pages</p>
  <a href="/css/default.asp" class="w3-btn w3-dark-grey">LEARN CSS</a>
  <a href="/cssref/default.asp" class="w3-btn w3-dark-grey">CSS REFERENCE</a>
 </div>
</div>

<div class="w3-row w3-light-grey w3-hide-large" style="padding-bottom:30px">
 <div class="w3-col l6 w3-center" style="padding:3%">
  <h1>CSS</h1>
  <p class="w3-xlarge">The language for styling web pages</p>
  <a href="/css/default.asp" class="w3-btn w3-dark-grey">LEARN CSS</a>
  <a href="/cssref/default.asp" class="w3-btn w3-dark-grey">CSS REFERENCE</a>
 </div>
 <div class="w3-col l6 w3-hide-small" style="padding-top:40px;padding:3%;">
  <h4>CSS Example:</h4>
  <div class="w3-code cssHigh notranslate w3-card-2 w3-border-green">
body {<br>
&nbsp;&nbsp;&nbsp; background-color: lightblue;<br>}<br>
h1 {<br>
&nbsp;&nbsp;&nbsp; color: white;<br>
&nbsp;&nbsp;&nbsp; text-align: center;<br>}<br>
p {<br>
&nbsp;&nbsp;&nbsp; font-family: verdana;<br>
&nbsp;&nbsp;&nbsp; font-size: 20px;<br>}
  </div>
  <a href="/css/tryit.asp?filename=trycss_default" target="_blank" class="w3-btn w3-theme w3-margin-0">Try it Yourself &raquo;</a>
 </div>
</div>

<div class="w3-row">
 <div class="w3-col l6 w3-center" style="padding:3%">
  <h1 class="w3-jumbo">JavaScript</h1>
  <p class="w3-xlarge w3-text-dark-grey">The language for programming web pages</p>
  <a href="/js/default.asp" class="w3-btn w3-dark-grey">LEARN JAVASCRIPT</a>
  <a href="/jsref/default.asp" class="w3-btn w3-dark-grey">JAVASCRIPT REFERENCE</a>
 </div>
 <div class="w3-col l6" style="padding:3%">
  <div class="w3-example w3-padding-16 w3-margin-0 w3-hide-small">
   <h4>JavaScript Example:</h4>
   <div class="w3-code notranslate w3-border-green">
   <div class="htmlHigh">
&lt;script&gt;
   </div>
   <div class="jsHigh">
function myFunction() {<br>
&nbsp;&nbsp;&nbsp; var x = document.getElementById(&quot;demo&quot;);<br>
&nbsp;&nbsp;&nbsp; x.style.fontSize = &quot;25px&quot;; <br>
&nbsp;&nbsp;&nbsp; x.style.color = &quot;red&quot;; <br>}<br>
    </div>
    <div class="htmlHigh">
&lt;/script&gt;<br><br>
&lt;button onclick=&quot;myFunction()&quot;&gt;Click Me!&lt;/button&gt;
    </div>
   </div>
   <a href="/js/tryit.asp?filename=tryjs_default" target="_blank" class="w3-btn w3-theme w3-margin-0">Try it Yourself &raquo;</a>
  </div>
 </div>
</div>

<div class="w3-row w3-light-grey">
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <div class="w3-card-2 w3-white">
   <h2 class="w3-xxlarge">SQL</h2>
   <p class="w3-text-dark-grey w3-large">A language for accessing databases</p>
   <a href="/sql/default.asp" class="w3-btn w3-dark-grey">LEARN SQL</a>
  </div>
 </div>
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <div class="w3-card-2 w3-white">
   <h2 class="w3-xxlarge">PHP</h2>
   <p class="w3-text-dark-grey w3-large">A web server programming language</p>
   <a href="/php/default.asp" class="w3-btn w3-dark-grey">LEARN PHP</a>
  </div>
 </div>
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <div class="w3-card-2 w3-white">
   <h2 class="w3-xxlarge">jQuery</h2>
   <p class="w3-text-dark-grey w3-large">A JavaScript library for developing web pages</p>
   <a href="/jquery/default.asp" class="w3-btn w3-dark-grey">LEARN JQUERY</a>
  </div>
 </div>
</div>
<div class="w3-row">
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <h2 class="w3-xxlarge">W3.CSS</h2>
  <p class="w3-large w3-text-dark-grey">A modern CSS framework for faster and better responsive web sites</p>
  <a href="/w3css/default.asp" class="w3-btn w3-dark-grey">LEARN W3.CSS</a>
 </div>
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <h2 class="w3-xxlarge">Color Picker</h2>
  <a href="/colors/colors_picker.asp" class="w3-hover-opacity"><img style="width:150px;height:128px;" src="/images/colorpicker.png" alt="Colorpicker"></a>
 </div>
 <div class="w3-col l4 w3-center" style="padding:3%;">
  <h2 class="w3-xxlarge">Bootstrap</h2>
  <p class="w3-large w3-text-dark-grey">Bootstrap is a CSS framework for designing better web pages</p>
  <a href="/bootstrap/default.asp" class="w3-btn w3-dark-grey">LEARN BOOTSTRAP</a>
 </div>
</div><br>

<footer class="w3-container w3-light-grey w3-center w3-padding-jumbo w3-padding-32 w3-text-grey">
 <a class="w3-btn w3-dark-grey w3-hover-black w3-hide-small" href="/cert/default.asp" style="font-size:16px">WEB CERTIFICATES</a><br>
 <a class="w3-btn w3-dark-grey w3-hover-black w3-hide-large w3-hide-medium" href="/cert/default.asp" style="font-size:16px;margin:0 -16px">WEB CERTIFICATES</a><br>
 <nav class="w3-right w3-hide-medium w3-hide-small w3-wide">
  <a href="/forum/default.asp" target="_blank" class="w3-hover-text-black" style="text-decoration:none">FORUM</a> |
  <a href="/about/default.asp" target="_top" class="w3-hover-text-black" style="text-decoration:none">ABOUT</a>
 </nav>
 <nav class="w3-center w3-hide-large w3-margin-top w3-wide">
  <a href="/forum/default.asp" target="_blank" class="w3-hover-text-black" style="text-decoration:none">FORUM</a> |
  <a href="/about/default.asp" target="_top" class="w3-hover-text-black" style="text-decoration:none">ABOUT</a>
 </nav>
 <hr>
 <p class="w3-medium w3-margin-top">
 W3Schools is optimized for learning, testing, and training. Examples might be simplified to improve reading and basic understanding. Tutorials, references, and examples are constantly reviewed to avoid errors, but we cannot warrant full correctness of
 all content. While using this site, you agree to have read and accepted our <a href="/about/about_copyright.asp" class="w3-hover-text-black">terms of use</a>, <a href="/about/about_privacy.asp" class="w3-hover-text-black">cookie and privacy policy</a>.<br>
 <a href="/about/about_copyright.asp" class="w3-hover-text-black">Copyright 1999-2016</a> by Refsnes Data. All Rights Reserved.<br><br></p>
 <a href="javascript:void(0);" onclick="clickFBLike()" title="Like W3Schools on Facebook" class="w3-hover-text-indigo">
 <i class="fa fa-thumbs-o-up w3-xxlarge"></i></a>
 <div id="fblikeframe" class="w3-modal">
  <div class="w3-modal-content w3-padding-64 w3-animate-zoom" id="popupDIV"></div>
 </div>
</footer>

<!-- END MAIN -->
</div>

<script>
function w3_open() {
  var x = document.getElementById("myAccordion");
  if (x.style.display === 'none') {
    x.style.display = 'block';
  } else {
    x.style.display = 'none';
  }
}
function w3_close() {
  document.getElementById("myAccordion").style.display = "none";
}
function open_xs_menu(x) {
  if (document.getElementById("sectionxs_" + x).innerHTML == "") {
    document.getElementById("sectionxs_" + x).innerHTML = document.getElementById("nav_" + x).innerHTML;
  } else {
    document.getElementById("sectionxs_" + x).innerHTML = "";
  }
}
function w3_open_nav(x) {
  if (document.getElementById("nav_" + x).style.display == "block") {
    w3_close_nav(x);
  } else {
    w3_close_nav("tutorials");
    w3_close_nav("references");
    w3_close_nav("examples");
    document.getElementById("nav_" + x).style.display = "block";
    if (document.getElementById("navbtn_" + x)) {
      document.getElementById("navbtn_" + x).getElementsByTagName("i")[0].style.display = "none";
      document.getElementById("navbtn_" + x).getElementsByTagName("i")[1].style.display = "inline";
    } 
    if (x == "search") {
      if (document.getElementById("gsc-i-id1")) {document.getElementById("gsc-i-id1").focus(); }
    }
  }
}
function w3_close_nav(x) {
  document.getElementById("nav_" + x).style.display = "none";
  if (document.getElementById("navbtn_" + x)) {
    document.getElementById("navbtn_" + x).getElementsByTagName("i")[0].style.display = "inline";
    document.getElementById("navbtn_" + x).getElementsByTagName("i")[1].style.display = "none";
  }
}
function open_translate(elmnt) {
  var a = document.getElementById("google_translate_element");
  if (a.style.display == "") {
    a.style.display = "none";
    elmnt.innerHTML = "<i class='fa'>&#xe801;</i>";
  } else {
    a.style.display = "";
    if (window.innerWidth > 830) {
      a.style.width = "20%";
    } else {
      a.style.width = "60%";
    }
    elmnt.innerHTML = "<span style='font-family:verdana;font-weight:bold;display:inline-block;width:21px;text-align:center;'>X</span>";
  }
}
function open_search(elmnt) {
  var a = document.getElementById("googleSearch");
  document.getElementById("navbtn_tutorials").style.visibility = "visible";
  if (a.style.display == "") {
    a.style.display = "none";
    elmnt.innerHTML = "<i class='fa'>&#xe802;</i>";    
  } else {
    a.style.display = "";  
    if (window.innerWidth > 1000) {
      a.style.width = "40%";
    } else if (window.innerWidth >768) {
      document.getElementById("navbtn_tutorials").style.visibility = "hidden";
      a.style.width = "80%";    
    } else {
      a.style.width = "80%";
    }
    if (document.getElementById("gsc-i-id1")) {document.getElementById("gsc-i-id1").focus(); }
    elmnt.innerHTML = "<span style='font-family:verdana;font-weight:bold;display:inline-block;width:23px;text-align:center;'>X</span>";
  }
}
function googleTranslateElementInit() {
	new google.translate.TranslateElement({
  pageLanguage: 'en',
  autoDisplay: false,    
  gaTrack: true,
  gaId: 'UA-3855518-1',
  layout: google.translate.TranslateElement.InlineLayout.SIMPLE
	}, 'google_translate_element');
}
function clickFBLike() {
  document.getElementById("fblikeframe").style.display='block';
  document.getElementById("popupDIV").innerHTML = "<iframe src='/fblike.asp?r=" + Math.random() + "' frameborder='no' style='height:200px;width:250px;'></iframe><br><button onclick='hideFBLike()' class='w3-btn w3-dark-grey w3-hover-black'>Close</button>";
}
function hideFBLike() {
  document.getElementById("fblikeframe").style.display='none';
}
</script>
<script src="/lib/w3codecolors.js"></script>
<script src="https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit"></script>

</body>
</html>