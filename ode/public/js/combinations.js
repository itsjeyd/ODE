// combinations.js --- Utilities for computing full set of output strings from a list of slots.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

var cart = function(slots) {
  if (slots.length === 0) {
    return [];
  } else if (slots.length === 1) {
    return slots.pop();
  } else if (slots.length === 2) {
    return combineSlots(slots[0], slots[1]);
  } else {
    var intermediateResult = combineSlots(slots[0], slots[1]);
    var remainingSlots = slots.slice(2);
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
