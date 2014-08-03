var cart = function(parts) {
  if (parts.length === 0) {
    return [];
  } else if (parts.length === 1) {
    return parts.pop();
  } else if (parts.length === 2) {
    return combineSlots(parts[0], parts[1]);
  } else {
    var intermediateResult = combineSlots(parts[0], parts[1]);
    var remainingSlots = parts.slice(2);
    return cart([intermediateResult].concat(remainingSlots));
  }
};

var combineSlots = function(slot1, slot2) {
  var acc = function(a, b, result) {
    if (a.length === 0) {
      return result
    } else {
      var intermediateResult = combineStrings(a[0], b);
      return acc(a.slice(1), b, result.concat(intermediateResult));
    }
  };
  if (slot1.length === 0 && slot2.length === 0) {
    return [];
  } else {
    return acc(slot1, slot2, []);
  }
};

var combineStrings = function(string, slot) {
  var acc = function(str, slt, result) {
    if (slt.length === 0) {
      return result
    } else {
      var concatenatedString = str + ' ' + slt[0];
      return acc(str, slt.slice(1), result.concat([concatenatedString]));
    }
  }
  if (slot.length === 0) {
    return [string];
  } else {
    return acc(string, slot, []);
  }
};
