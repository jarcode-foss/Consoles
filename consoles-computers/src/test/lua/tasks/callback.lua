
return function()
    local func = function(x, y) return x + y end
    submitCallback(func)
    submitPartialCallback(func)
    submitValueCallback(func)
end