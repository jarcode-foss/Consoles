function dumptable(prefix, tbl)
    for k, v in pairs(tbl) do
        print(prefix .. "." .. tostring(k) .. " = " .. tostring(v) .. ", " .. type(v));
    end
end

return function()
    dumptable(stringArray())
    dumptable(objectArray())
end