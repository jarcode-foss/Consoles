
ffi = require("ffi")

return function()
    print(tostring(ffi) .. ", " .. type(ffi))
    for k, v in pairs(ffi) do
        print("ffi." .. k .. " = " .. tostring(v) .. ", " .. type(v));
    end
    ffi.cdef([[
    int printf(const char* fmt, ...);
    ]])
    ffi.C.printf("L->C: testing printf from C")
end