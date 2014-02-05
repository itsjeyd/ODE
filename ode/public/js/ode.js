// Source: http://viralpatel.net/blogs/jquery-get-text-element-without-child-element/
jQuery.fn.textOnly = function() {
  return $(this)
    .clone()
    .children()
    .remove()
    .end() // Go back to cloned element
    .text();
};
